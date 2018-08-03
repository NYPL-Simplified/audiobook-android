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
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventChapterCompleted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventChapterWaiting
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackPaused
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackProgressUpdate
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStarted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackStopped
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPlaybackRate.NORMAL_TIME
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateInitial
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStatePlaying
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateStopped
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateWaitingForElement
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.NextChapterStatus.NEXT_CHAPTER_NEVER
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.NextChapterStatus.NEXT_CHAPTER_NOT_DOWNLOADED
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.NextChapterStatus.NEXT_CHAPTER_READY
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Subscription
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
     * The initial state; no spine element is selected, the player is not playing.
     */

    object ExoPlayerStateInitial : ExoPlayerState()

    /*
     * The player is currently playing the given spine element.
     */

    data class ExoPlayerStatePlaying(
      var spineElement: ExoSpineElement,
      val observerTask: ScheduledFuture<*>)
      : ExoPlayerState()

    /*
     * The player is waiting until the given spine element is downloaded before continuing playback.
     */

    data class ExoPlayerStateWaitingForElement(
      var spineElement: ExoSpineElement)
      : ExoPlayerState()

    /*
     * The player was playing the given spine element but is currently paused.
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
  private lateinit var downloadEventSubscription: Subscription
  private val allocator: Allocator = DefaultAllocator(this.bufferSegmentSize)

  /*
   * A listener registered with the underlying ExoPlayer instance to observe state changes.
   */

  private val exoPlayerEventListener = object : ExoPlayer.Listener {
    override fun onPlayerError(error: ExoPlaybackException?) {
      this@ExoAudioBookPlayer.log.error("onPlayerError: ", error)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, stateNow: Int) {
      val stateName = this.stateName(stateNow)
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
    this.exoPlayer.addListener(this.exoPlayerEventListener)
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

  /**
   * A playback observer that is called repeatedly to observe the current state of the player.
   */

  private inner class PlaybackObserver(private val spineElement: ExoSpineElement) : Runnable {
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
        PlayerEventPlaybackProgressUpdate(this.spineElement, position.toInt()))

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
        this.bufferSegmentCount * this.bufferSegmentSize,
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
        AudioCapabilities.getCapabilities(this.context),
        AudioManager.STREAM_MUSIC)

    this.exoPlayer.prepare(audioRenderer)
    this.exoPlayer.playWhenReady = true
    this.currentPlaybackOffset = 0L

    return this.schedulePlaybackObserverForSpineElement(spineElement)
  }

  /**
   * Schedule a playback observer that will check the current state of playback once per second.
   */

  private fun schedulePlaybackObserverForSpineElement(
    spineElement: ExoSpineElement): ScheduledFuture<*> {
    return this.engineExecutor.scheduleAtFixedRate(
      this.PlaybackObserver(spineElement),
      1L,
      1L,
      TimeUnit.SECONDS)
  }

  /**
   * The download status of a spine element has changed. We only actually care about
   * the spine element that we're either currently playing or are currently waiting for.
   * Everything else is uninteresting.
   */

  private fun onDownloadStatusChanged(status: PlayerSpineElementDownloadStatus) {
    ExoEngineThread.checkIsExoEngineThread()

    val currentState = this.state
    return when (currentState) {
      ExoPlayerStateInitial -> Unit

      /*
       * If the we're playing the current spine element, and the status is anything other
       * than "downloaded", stop everything.
       */

      is ExoPlayerStatePlaying -> {
        if (currentState.spineElement == status.spineElement) {
          when (status) {
            is PlayerSpineElementNotDownloaded,
            is PlayerSpineElementDownloading,
            is PlayerSpineElementDownloadFailed -> this.playNothing()
            is PlayerSpineElementDownloaded -> Unit
          }
        } else {

        }
      }

      /*
       * If the we're stopped on the current spine element, and the status is anything other
       * than "downloaded", stop everything.
       */

      is ExoPlayerStateStopped -> {
        if (currentState.spineElement == status.spineElement) {
          when (status) {
            is PlayerSpineElementNotDownloaded,
            is PlayerSpineElementDownloading,
            is PlayerSpineElementDownloadFailed -> this.playNothing()
            is PlayerSpineElementDownloaded -> Unit
          }
        } else {

        }
      }

      /*
       * If we're waiting for the spine element in question, and the status is now "downloaded",
       * then start playing.
       */

      is ExoPlayerStateWaitingForElement -> {
        if (currentState.spineElement == status.spineElement) {
          when (status) {
            is PlayerSpineElementNotDownloaded,
            is PlayerSpineElementDownloading,
            is PlayerSpineElementDownloadFailed -> Unit
            is PlayerSpineElementDownloaded -> this.opPlay()
          }
        } else {

        }
      }
    }
  }

  private fun playNothing() {
    this.log.debug("playNothing")

    val currentState = this.state
    return when (currentState) {
      ExoPlayerStateInitial -> Unit

      is ExoPlayerStatePlaying -> {
        currentState.observerTask.cancel(true)
        this.exoPlayer.stop()
        this.exoPlayer.seekTo(0L)
        this.currentPlaybackOffset = 0

        synchronized(this.stateLock) { this.state = ExoPlayerStateInitial }
        this.statusEvents.onNext(PlayerEventPlaybackStopped(
          currentState.spineElement, 0))
      }

      is ExoPlayerStateWaitingForElement -> {
        this.exoPlayer.stop()
        this.exoPlayer.seekTo(0L)
        this.currentPlaybackOffset = 0

        synchronized(this.stateLock) { this.state = ExoPlayerStateInitial }
        this.statusEvents.onNext(PlayerEventPlaybackStopped(
          currentState.spineElement, 0))
      }

      is ExoPlayerStateStopped -> {
        this.exoPlayer.stop()
        this.exoPlayer.seekTo(0L)
        this.currentPlaybackOffset = 0

        synchronized(this.stateLock) { this.state = ExoPlayerStateInitial }
        this.statusEvents.onNext(PlayerEventPlaybackStopped(
          currentState.spineElement, 0))
      }
    }
  }

  private fun playNextSpineElementIfAvailable(element: ExoSpineElement): NextChapterStatus {
    this.log.debug("playNextSpineElementIfAvailable: {}", element.index)

    val next = element.next as ExoSpineElement?
    if (next == null) {
      this.log.debug("spine element {} has no next element", element.index)
      return NEXT_CHAPTER_NEVER
    }

    val downloadStatus = next.downloadStatus
    return when (downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadFailed -> {
        this.log.debug("spine element {} is not downloaded ({}), cannot continue",
          next.index, downloadStatus)

        val newState = ExoPlayerStateWaitingForElement(spineElement = next)
        synchronized(this.stateLock) { this.state = newState }
        this.statusEvents.onNext(PlayerEventChapterWaiting(next))

        NEXT_CHAPTER_NOT_DOWNLOADED
      }

      is PlayerSpineElementDownloaded -> {
        this.playSpineElementFromStart(next)
        NEXT_CHAPTER_READY
      }
    }
  }

  private fun playSpineElementFromStart(element: ExoSpineElement) {
    this.log.debug("playSpineElementFromStart: {}", element.index)

    val newState = ExoPlayerStatePlaying(
      spineElement = element,
      observerTask = this.openSpineElement(element))

    synchronized(this.stateLock) { this.state = newState }
    this.statusEvents.onNext(PlayerEventPlaybackStarted(element, 0))
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
        this.opPlayInitial()
      }
      is ExoPlayerStatePlaying -> {
        this.log.debug("not playing in the playing state")
      }
      is ExoPlayerStateStopped -> {
        this.opPlayStopped(state)
      }
      is ExoPlayerStateWaitingForElement -> {
        this.opPlayWaitingForElement(state)
      }
    }
  }

  private fun opPlayStopped(state: ExoPlayerStateStopped) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayStopped")

    this.exoPlayer.playWhenReady = true

    synchronized(this.stateLock) {
      this.state = ExoPlayerStatePlaying(
        spineElement = state.spineElement,
        observerTask = this.schedulePlaybackObserverForSpineElement(spineElement = state.spineElement))
    }

    this.statusEvents.onNext(PlayerEventPlaybackStarted(
      state.spineElement, this.currentPlaybackOffset.toInt()))
  }

  private fun opPlayWaitingForElement(state: ExoPlayerStateWaitingForElement) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayWaitingForElement")

    val downloadStatus = state.spineElement.downloadStatus
    return when (downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadFailed -> {
        this.log.debug("spine element {} is not downloaded ({}), cannot continue",
          state.spineElement.index, downloadStatus)
      }

      is PlayerSpineElementDownloaded -> {
        this.playSpineElementFromStart(state.spineElement)
      }
    }
  }

  private fun opPlayInitial() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayInitial")

    val firstElement = this.book.spine.firstOrNull()
    if (firstElement == null) {
      return this.log.debug("no available initial spine element")
    }

    val downloadStatus = firstElement.downloadStatus
    return when (downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadFailed -> {
        this.log.debug("spine element {} is not downloaded ({}), cannot continue",
          firstElement.index, downloadStatus)
      }

      is PlayerSpineElementDownloaded -> {
        this.playSpineElementFromStart(firstElement)
      }
    }
  }

  private fun opCurrentTrackFinished() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opCurrentTrackFinished")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial,
      is ExoPlayerStateStopped -> {
        this.log.error("current track is finished but the player thinks it is not playing!")
        throw Unimplemented()
      }

      is ExoPlayerStatePlaying -> {
        this.statusEvents.onNext(PlayerEventChapterCompleted(state.spineElement))

        this.opPausePlaying(state)
        when (this.opSkipToNextChapter()) {
          NEXT_CHAPTER_NOT_DOWNLOADED -> throw Unimplemented()
          NEXT_CHAPTER_NEVER -> throw Unimplemented()
          NEXT_CHAPTER_READY -> this.opPlay()
        }
      }

      is ExoPlayerStateWaitingForElement ->
        throw Unimplemented()
    }
  }

  private enum class NextChapterStatus {
    NEXT_CHAPTER_NOT_DOWNLOADED,
    NEXT_CHAPTER_NEVER,
    NEXT_CHAPTER_READY
  }

  private fun opSkipToNextChapter(): NextChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapter")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial ->
        throw Unimplemented()
      is ExoPlayerStatePlaying ->
        this.opSkipToNextChapterPlaying(state)
      is ExoPlayerStateStopped ->
        this.opSkipToNextChapterStopped(state)
      is ExoPlayerStateWaitingForElement ->
        throw Unimplemented()
    }
  }

  private fun opSkipToNextChapterStopped(state: ExoPlayerStateStopped): NextChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapterStopped")
    return this.playNextSpineElementIfAvailable(state.spineElement)
  }

  private fun opSkipToNextChapterPlaying(state: ExoPlayerStatePlaying): NextChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapterPlaying")
    return this.playNextSpineElementIfAvailable(state.spineElement)
  }

  private fun opPause() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPause")

    val state = synchronized(this.stateLock) { this.state }
    return when (state) {
      is ExoPlayerStateInitial ->
        this.log.debug("not pausing in the initial state")
      is ExoPlayerStatePlaying ->
        this.opPausePlaying(state)
      is ExoPlayerStateStopped ->
        this.log.debug("not pausing in the stopped state")
      is ExoPlayerStateWaitingForElement ->
        this.log.debug("not pausing in the waiting state")
    }
  }

  private fun opPausePlaying(state: ExoPlayerStatePlaying) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPausePlaying")

    state.observerTask.cancel(true)
    this.exoPlayer.playWhenReady = false

    synchronized(this.stateLock) {
      this.state = ExoPlayerStateStopped(spineElement = state.spineElement)
    }

    this.statusEvents.onNext(PlayerEventPlaybackPaused(
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

    /*
     * Subscribe to notifications of download changes. This is only needed because the
     * player may be waiting for a chapter to be downloaded and needs to know when it can
     * safely play the chapter.
     */

    this.downloadEventSubscription =
      this.book.spineElementDownloadStatus.subscribe(
        { status -> this.engineExecutor.execute { this.onDownloadStatusChanged(status) } },
        { exception -> this.log.error("download status error: ", exception) })
  }

  override val isPlaying: Boolean
    get() =
      when (synchronized(this.stateLock) { this.state }) {
        is ExoPlayerStateInitial -> false
        is ExoPlayerStatePlaying -> true
        is ExoPlayerStateStopped -> false
        is ExoPlayerStateWaitingForElement -> true
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