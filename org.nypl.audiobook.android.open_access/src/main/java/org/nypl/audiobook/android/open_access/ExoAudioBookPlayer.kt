package org.nypl.audiobook.android.open_access

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.ExoPlayer
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer
import com.google.android.exoplayer.MediaCodecSelector
import com.google.android.exoplayer.audio.AudioCapabilities
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.Allocator
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultUriDataSource
import net.jcip.annotations.GuardedBy
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStarted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStopped
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPlaybackRate.NORMAL_TIME
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateInitial
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStatePlaying
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateStopped
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * An ExoPlayer player.
 */

class ExoAudioBookPlayer private constructor(
  private val engineProvider: ExoEngineProvider,
  private val engineExecutor: ScheduledExecutorService,
  private val context: Context,
  private val statusEvents: PublishSubject<PlayerEvent>,
  private val exoPlayer: ExoPlayer)
  : PlayerType {

  private val log = LoggerFactory.getLogger(ExoAudioBookPlayer::class.java)

  private val bufferSegmentSize = 64 * 1024
  private val bufferSegmentCount = 256
  private val userAgent = "${this.engineProvider.name()}/${this.engineProvider.version()}"

  /*
   * The current playback state.
   */

  private sealed class ExoPlayerState {

    /*
     * The initial state; no spine item is selected, the player is not playing.
     */

    object ExoPlayerStateInitial : ExoPlayerState()

    /*
     * The player is currently playing the given spine item.
     */

    data class ExoPlayerStatePlaying(
      var spineElement: ExoSpineElement,
      val observerTask: ScheduledFuture<*>)
      : ExoPlayerState()

    /*
     * The player was playing the given spine item but is currently paused.
     */

    data class ExoPlayerStateStopped(
      var spineElement: ExoSpineElement)
      : ExoPlayerState()

  }

  @Volatile
  private var currentPlaybackRate: PlayerPlaybackRate = NORMAL_TIME

  @Volatile
  private var currentPlaybackOffset: Long = 0

  private val stateLock: Any = Object()
  @GuardedBy("stateLock")
  private var state: ExoPlayerState = ExoPlayerStateInitial

  private lateinit var book: ExoAudioBook
  private val allocator: Allocator = DefaultAllocator(this.bufferSegmentSize)

  /*
   * A listener registered with the underlying ExoPlayer instance to observe state changes.
   */

  private val exoPlayerEventListener = object : ExoPlayer.Listener {
    override fun onPlayerError(error: ExoPlaybackException?) {
      this@ExoAudioBookPlayer.log.error("onPlayerError: ", error)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, stateNow: Int) {
      val stateName = stateName(stateNow)
      this@ExoAudioBookPlayer.log.debug(
        "onPlayerStateChanged: {} {} ({})", playWhenReady, stateName, stateNow)
    }

    private fun stateName(playbackState: Int): String {
      return when (playbackState) {
        ExoPlayer.STATE_BUFFERING -> "buffering"
        ExoPlayer.STATE_ENDED -> "ended"
        ExoPlayer.STATE_IDLE -> "idle"
        ExoPlayer.STATE_PREPARING -> "preparing"
        ExoPlayer.STATE_READY -> "ready"
        else -> "unrecognized state"
      }
    }

    override fun onPlayWhenReadyCommitted() {
      this@ExoAudioBookPlayer.log.debug("onPlayWhenReadyCommitted")
    }
  }

  init {
    this.exoPlayer.addListener(exoPlayerEventListener)
  }

  companion object {

    fun create(
      engineProvider: ExoEngineProvider,
      context: Context,
      engineExecutor: ScheduledExecutorService): ExoAudioBookPlayer {

      val statusEvents = PublishSubject.create<PlayerEvent>()

      /*
       * Initialize the audio player on the engine thread.
       */

      return engineExecutor.submit(Callable {

        /*
         * The rendererCount parameter is not well documented. It appears to be the number of
         * renderers that are required to render a single track. To render a piece of video that
         * had video, audio, and a subtitle track would require three renderers. Audio books should
         * require just one.
         */

        val player =
          ExoPlayer.Factory.newInstance(1)

        return@Callable ExoAudioBookPlayer(
          context = context,
          engineProvider = engineProvider,
          engineExecutor = engineExecutor,
          exoPlayer = player,
          statusEvents = statusEvents)

      }).get(5L, TimeUnit.SECONDS)
    }
  }

  private fun opSetPlaybackRate(new_rate: PlayerPlaybackRate) {
    ExoEngineThread.checkIsExoEngineThread()

    this.currentPlaybackRate = new_rate
  }

  private fun opPlay() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlay")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial -> {
        opPlayInitial(state)
      }
      is ExoPlayerStatePlaying -> {
        this.log.debug("not playing in the playing state")
      }
      is ExoPlayerStateStopped -> {
        TODO("Playing whilst stopped is not implemented")
      }
    }
  }

  private fun opPlayInitial(state: ExoPlayerStateInitial) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayInitial")

    val firstElement = this.book.spine.firstOrNull()
    if (firstElement == null) {
      return this.log.debug("no available initial spine element")
    }

    return when (firstElement.downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadFailed -> {
        return this.log.debug("spine element is not downloaded")
      }

      is PlayerSpineElementDownloaded -> {
        val new_state = ExoPlayerStatePlaying(
          spineElement = firstElement,
          observerTask = openSpineElement(firstElement))

        synchronized(this.stateLock) { this.state = new_state }
        this.statusEvents.onNext(PlayerEventPlaybackStarted(firstElement, 0))
      }
    }
  }

  /**
   * A playback observer that is called repeatedly to observe the current state of the player.
   */

  private inner class PlaybackObserver(
    private val spineElement: ExoSpineElement) : Runnable {

    private var gracePeriod: Int = 1

    override fun run() {
      val bookPlayer = this@ExoAudioBookPlayer
      val duration = bookPlayer.exoPlayer.duration
      val position = bookPlayer.exoPlayer.currentPosition
      bookPlayer.log.debug("playback: {}/{}", position, duration)

      /*
       * Report the current playback status.
       */

      bookPlayer.currentPlaybackOffset = position
      bookPlayer.statusEvents.onNext(
        PlayerEvent.PlayerEventPlaybackProgressUpdate(this.spineElement, position.toInt()))

      /*
       * Provide a short grace period before indicating that the current spine element has
       * finished playing. This avoids a situation where the player indicates that it is at
       * the end of the audio but the sound hardware has not actually finished playing the last
       * chunk.
       */

      if (position >= duration) {
        if (this.gracePeriod == 0) {
          bookPlayer.currentPlaybackOffset = this.spineElement.duration.millis
          bookPlayer.engineExecutor.execute {
            bookPlayer.opCurrentTrackFinished()
          }
        }
        --this.gracePeriod
      }
    }
  }

  private fun openSpineElement(spineElement: ExoSpineElement): ScheduledFuture<*> {
    this.log.debug("openSpineElement: {}", spineElement.index)

    /*
     * Set up an audio renderer for the spine element and tell ExoPlayer to prepare it and then
     * play when ready.
     */

    val dataSource =
      DefaultUriDataSource(this.context, null, this.userAgent)

    val uri =
      Uri.fromFile(spineElement.partFile)

    val sampleSource =
      ExtractorSampleSource(
        uri,
        dataSource,
        this.allocator,
        bufferSegmentCount * bufferSegmentSize,
        null,
        null,
        0)

    val audioRenderer =
      MediaCodecAudioTrackRenderer(
        sampleSource,
        MediaCodecSelector.DEFAULT,
        null,
        true,
        null,
        null,
        AudioCapabilities.getCapabilities(context),
        AudioManager.STREAM_MUSIC)

    this.exoPlayer.prepare(audioRenderer)
    this.exoPlayer.playWhenReady = true
    this.currentPlaybackOffset = 0L

    /*
     * Schedule a playback observer that will check the current state of playback once per second.
     */

    return this.engineExecutor.scheduleAtFixedRate(
      PlaybackObserver(spineElement),
      1L,
      1L,
      TimeUnit.SECONDS)
  }

  private fun opCurrentTrackFinished() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opCurrentTrackFinished")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial,
      is ExoPlayerStateStopped -> {
        this.log.error("current track is finished but the player thinks it is not playing!")
      }

      is ExoPlayerStatePlaying -> {
        this.opPausePlaying(state)
        if (opSkipToNextChapter()) {
          opPlay()
        } else {
          this.log.debug("next chapter is not available")
        }
      }
    }
  }

  private fun opSkipToNextChapter(): Boolean {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapter")
    return false
  }

  private fun opPause() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPause")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial -> {
        this.log.debug("not pausing in the initial state")
      }

      is ExoPlayerStatePlaying -> {
        opPausePlaying(state)
      }

      is ExoPlayerStateStopped -> {
        this.log.debug("not pausing in the stopped state")
      }
    }
  }

  private fun opPausePlaying(state: ExoPlayerStatePlaying) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPausePlaying")

    state.observerTask.cancel(true)
    this.exoPlayer.stop()

    synchronized(this.stateLock) {
      this.state = ExoPlayerStateStopped(spineElement = state.spineElement)
    }

    this.statusEvents.onNext(PlayerEventPlaybackStopped(
      state.spineElement, this.currentPlaybackOffset.toInt()))
  }

  private fun opSkipToPreviousChapter(): Boolean {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToPreviousChapter")
    return false
  }

  private fun opSkipForward() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipForward")
  }

  private fun opSkipBack() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipBack")
  }

  private fun opPlayAtLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayAtLocation: {}", location)
  }

  private fun opMovePlayheadToLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opMovePlayheadToLocation: {}", location)
  }

  private fun opSetBook(book: ExoAudioBook) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSetBook: {}", book)
    this.book = book
  }

  override val isPlaying: Boolean
    get() =
      when (synchronized(this.stateLock) { this.state }) {
        is ExoPlayerStateInitial -> false
        is ExoPlayerStatePlaying -> true
        is ExoPlayerStateStopped -> false
      }

  override var playbackRate: PlayerPlaybackRate
    get() =
      this.currentPlaybackRate
    set(value) {
      this.engineExecutor.execute { this.opSetPlaybackRate(value) }
    }

  override val events: Observable<PlayerEvent>
    get() = this.statusEvents

  override fun play() {
    this.engineExecutor.execute { this.opPlay() }
  }

  override fun pause() {
    this.engineExecutor.execute { this.opPause() }
  }

  override fun skipToNextChapter() {
    this.engineExecutor.execute { this.opSkipToNextChapter() }
  }

  override fun skipToPreviousChapter() {
    this.engineExecutor.execute { this.opSkipToPreviousChapter() }
  }

  override fun skipForward() {
    this.engineExecutor.execute { this.opSkipForward() }
  }

  override fun skipBack() {
    this.engineExecutor.execute { this.opSkipBack() }
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.engineExecutor.execute { this.opPlayAtLocation(location) }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.engineExecutor.execute { this.opMovePlayheadToLocation(location) }
  }

  fun setBook(book: ExoAudioBook) {
    this.engineExecutor.execute { this.opSetBook(book) }
  }
}