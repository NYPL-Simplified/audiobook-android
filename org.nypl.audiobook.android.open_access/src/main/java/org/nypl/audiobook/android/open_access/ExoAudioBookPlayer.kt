package org.nypl.audiobook.android.open_access

import android.content.Context
import android.media.AudioManager
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
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
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_NONEXISTENT
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_NOT_DOWNLOADED
import org.nypl.audiobook.android.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_READY
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Subscription
import rx.subjects.PublishSubject
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An ExoPlayer player.
 */

class ExoAudioBookPlayer private constructor(
  private val engineProvider: ExoEngineProvider,
  private val engineExecutor: ScheduledExecutorService,
  private val context: Context,
  private val statusEvents: PublishSubject<PlayerEvent>,
  private val book: ExoAudioBook,
  private val exoPlayer: ExoPlayer)
  : PlayerType {

  private val log = LoggerFactory.getLogger(ExoAudioBookPlayer::class.java)
  private val bufferSegmentSize = 64 * 1024
  private val bufferSegmentCount = 256
  private val userAgent = "${this.engineProvider.name()}/${this.engineProvider.version()}"
  private val closed = AtomicBoolean(false)

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

  private val downloadEventSubscription: Subscription
  private val allocator: Allocator = DefaultAllocator(this.bufferSegmentSize)
  private lateinit var exoAudioRenderer: MediaCodecAudioTrackRenderer

  private fun stateSet(state: ExoPlayerState) {
    synchronized(this.stateLock) { this.state = state }
  }

  private fun stateGet(): ExoPlayerState =
    synchronized(this.stateLock) { this.state }

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

  companion object {

    fun create(
      book: ExoAudioBook,
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
          book = book,
          context = context,
          engineProvider = engineProvider,
          engineExecutor = engineExecutor,
          exoPlayer = player,
          statusEvents = statusEvents)

      }).get(5L, SECONDS)
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

  private fun openSpineElement(
    spineElement: ExoSpineElement,
    offset: Int = 0): ScheduledFuture<*> {
    this.log.debug("openSpineElement: {} (offset {})", spineElement.index, offset)

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

    this.exoAudioRenderer =
      MediaCodecAudioTrackRenderer(
        sampleSource,
        MediaCodecSelector.DEFAULT,
        null,
        true,
        null,
        null,
        AudioCapabilities.getCapabilities(this.context),
        AudioManager.STREAM_MUSIC)

    val offsetMs = offset.toLong()
    this.exoPlayer.prepare(this.exoAudioRenderer)
    this.exoPlayer.playWhenReady = true
    this.exoPlayer.seekTo(offsetMs)
    this.currentPlaybackOffset = offsetMs
    this.setPlayerPlaybackRate(this.currentPlaybackRate)
    return this.schedulePlaybackObserverForSpineElement(spineElement)
  }

  /**
   * Schedule a playback observer that will check the current state of playback once per second.
   */

  private fun schedulePlaybackObserverForSpineElement(
    spineElement: ExoSpineElement): ScheduledFuture<*> {
    return this.engineExecutor.scheduleAtFixedRate(
      this.PlaybackObserver(spineElement), 1L, 1L, SECONDS)
  }

  /**
   * The download status of a spine element has changed. We only actually care about
   * the spine element that we're either currently playing or are currently waiting for.
   * Everything else is uninteresting.
   */

  private fun onDownloadStatusChanged(status: PlayerSpineElementDownloadStatus) {
    ExoEngineThread.checkIsExoEngineThread()

    val currentState = this.stateGet()
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
            is PlayerSpineElementDownloaded -> {
              this.playSpineElementIfAvailable(currentState.spineElement)
              Unit
            }
          }
        } else {

        }
      }
    }
  }

  /**
   * Configure the current player to use the given playback rate.
   */

  private fun setPlayerPlaybackRate(newRate: PlayerPlaybackRate) {
    if (Build.VERSION.SDK_INT >= 23) {
      val params = PlaybackParams()
      params.speed = newRate.speed.toFloat()
      this.exoPlayer.sendMessage(this.exoAudioRenderer, 2, params)
    }
  }

  /**
   * Forcefully stop playback and reset the player.
   */

  private fun playNothing() {
    this.log.debug("playNothing")

    fun resetPlayer() {
      this.log.debug("playNothing: resetting player")
      this.exoPlayer.stop()
      this.exoPlayer.seekTo(0L)
      this.currentPlaybackOffset = 0
      this.stateSet(ExoPlayerStateInitial)
    }

    val currentState = this.stateGet()
    return when (currentState) {
      ExoPlayerStateInitial -> resetPlayer()

      is ExoPlayerStatePlaying -> {
        currentState.observerTask.cancel(true)
        resetPlayer()
        this.statusEvents.onNext(PlayerEventPlaybackStopped(currentState.spineElement, 0))
      }

      is ExoPlayerStateWaitingForElement -> {
        resetPlayer()
        this.statusEvents.onNext(PlayerEventPlaybackStopped(currentState.spineElement, 0))
      }

      is ExoPlayerStateStopped -> {
        resetPlayer()
        this.statusEvents.onNext(PlayerEventPlaybackStopped(currentState.spineElement, 0))
      }
    }
  }

  private fun playFirstSpineElementIfAvailable(): SkipChapterStatus {
    this.log.debug("playFirstSpineElementIfAvailable")

    val firstElement = this.book.spine.firstOrNull()
    if (firstElement == null) {
      this.log.debug("no available initial spine element")
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return playSpineElementIfAvailable(firstElement)
  }

  private fun playLastSpineElementIfAvailable(): SkipChapterStatus {
    this.log.debug("playLastSpineElementIfAvailable")

    val lastElement = this.book.spine.lastOrNull()
    if (lastElement == null) {
      this.log.debug("no available final spine element")
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return playSpineElementIfAvailable(lastElement)
  }

  private fun playNextSpineElementIfAvailable(element: ExoSpineElement): SkipChapterStatus {
    this.log.debug("playNextSpineElementIfAvailable: {}", element.index)

    val next = element.next as ExoSpineElement?
    if (next == null) {
      this.log.debug("spine element {} has no next element", element.index)
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return playSpineElementIfAvailable(next)
  }

  private fun playPreviousSpineElementIfAvailable(element: ExoSpineElement): SkipChapterStatus {
    this.log.debug("playPreviousSpineElementIfAvailable: {}", element.index)

    val previous = element.previous as ExoSpineElement?
    if (previous == null) {
      this.log.debug("spine element {} has no previous element", element.index)
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return playSpineElementIfAvailable(previous)
  }

  private fun playSpineElementIfAvailable(
    element: ExoSpineElement,
    offset: Int = 0): SkipChapterStatus {
    this.log.debug("playSpineElementIfAvailable: {}", element.index)
    this.playNothing()

    val downloadStatus = element.downloadStatus
    return when (downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadFailed -> {
        this.log.debug("playSpineElementIfAvailable: spine element {} is not downloaded ({}), cannot continue",
          element.index, downloadStatus)

        this.stateSet(ExoPlayerStateWaitingForElement(spineElement = element))
        this.statusEvents.onNext(PlayerEventChapterWaiting(element))
        SKIP_TO_CHAPTER_NOT_DOWNLOADED
      }

      is PlayerSpineElementDownloaded -> {
        this.playSpineElementUnconditionally(element, offset)
        SKIP_TO_CHAPTER_READY
      }
    }
  }

  private fun playSpineElementUnconditionally(element: ExoSpineElement, offset: Int = 0) {
    this.log.debug("playSpineElementUnconditionally: {}", element.index)

    this.stateSet(ExoPlayerStatePlaying(
      spineElement = element,
      observerTask = this.openSpineElement(element, offset)))
    this.statusEvents.onNext(PlayerEventPlaybackStarted(element, offset))
  }

  private fun opSetPlaybackRate(newRate: PlayerPlaybackRate) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSetPlaybackRate: {}", newRate)

    this.currentPlaybackRate = newRate
    this.setPlayerPlaybackRate(newRate)
  }

  private fun opPlay() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlay")

    val state = this.stateGet()
    return when (state) {
      is ExoPlayerStateInitial -> {
        this.playFirstSpineElementIfAvailable()
        Unit
      }

      is ExoPlayerStatePlaying ->
        this.log.debug("opPlay: already playing")

      is ExoPlayerStateStopped ->
        this.opPlayStopped(state)

      is ExoPlayerStateWaitingForElement -> {
        this.playSpineElementIfAvailable(state.spineElement)
        Unit
      }
    }
  }

  private fun opPlayStopped(state: ExoPlayerStateStopped) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayStopped")

    this.exoPlayer.playWhenReady = true

    this.stateSet(ExoPlayerStatePlaying(
      spineElement = state.spineElement,
      observerTask = this.schedulePlaybackObserverForSpineElement(spineElement = state.spineElement)))

    this.statusEvents.onNext(PlayerEventPlaybackStarted(
      state.spineElement, this.currentPlaybackOffset.toInt()))
  }

  private fun opCurrentTrackFinished() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opCurrentTrackFinished")

    val state = this.stateGet()
    return when (state) {
      is ExoPlayerStateInitial,
      is ExoPlayerStateWaitingForElement,
      is ExoPlayerStateStopped -> {
        this.log.error("current track is finished but the player thinks it is not playing!")
        throw Unimplemented()
      }

      is ExoPlayerStatePlaying -> {
        this.statusEvents.onNext(PlayerEventChapterCompleted(state.spineElement))
        this.playNextSpineElementIfAvailable(state.spineElement)
        Unit
      }
    }
  }

  /**
   * The status of an attempt to switch to a chapter.
   */

  private enum class SkipChapterStatus {

    /**
     * The chapter is not downloaded and therefore cannot be played at the moment.
     */

    SKIP_TO_CHAPTER_NOT_DOWNLOADED,

    /**
     * The chapter does not exist and will never exist.
     */

    SKIP_TO_CHAPTER_NONEXISTENT,

    /**
     * The chapter exists and is ready for playback.
     */

    SKIP_TO_CHAPTER_READY
  }

  private fun opSkipToNextChapter(): SkipChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapter")

    val state = this.stateGet()
    return when (state) {
      is ExoPlayerStateInitial ->
        this.playFirstSpineElementIfAvailable()
      is ExoPlayerStatePlaying ->
        this.playNextSpineElementIfAvailable(state.spineElement)
      is ExoPlayerStateStopped ->
        this.playNextSpineElementIfAvailable(state.spineElement)
      is ExoPlayerStateWaitingForElement ->
        this.playNextSpineElementIfAvailable(state.spineElement)
    }
  }

  private fun opSkipToPreviousChapter(): SkipChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToPreviousChapter")

    val state = this.stateGet()
    return when (state) {
      ExoPlayerStateInitial ->
        this.playLastSpineElementIfAvailable()
      is ExoPlayerStatePlaying ->
        this.playPreviousSpineElementIfAvailable(state.spineElement)
      is ExoPlayerStateWaitingForElement ->
        this.playPreviousSpineElementIfAvailable(state.spineElement)
      is ExoPlayerStateStopped ->
        this.playPreviousSpineElementIfAvailable(state.spineElement)
    }
  }

  private fun opPause() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPause")

    val state = this.stateGet()
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

    this.stateSet(ExoPlayerStateStopped(spineElement = state.spineElement))
    this.statusEvents.onNext(PlayerEventPlaybackPaused(
      state.spineElement, this.currentPlaybackOffset.toInt()))
  }

  private fun opSkipForward() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipForward")

    val offset = Math.min(this.exoPlayer.duration, this.exoPlayer.currentPosition + 15_000L)
    this.exoPlayer.seekTo(offset)
    this.currentPlaybackOffset = offset
  }

  private fun opSkipBack() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipBack")

    val offset = Math.max(0L, this.exoPlayer.currentPosition - 15_000L)
    this.exoPlayer.seekTo(offset)
    this.currentPlaybackOffset = offset
  }

  private fun opPlayAtLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayAtLocation: {}", location)

    val spineElement =
      this.book.spineElementForPartAndChapter(location.part, location.chapter)
        as ExoSpineElement?

    if (spineElement == null) {
      return this.log.debug("spine element does not exist")
    }

    this.playSpineElementIfAvailable(spineElement, location.offsetMilliseconds)
  }

  private fun opMovePlayheadToLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opMovePlayheadToLocation: {}", location)
    this.opPlayAtLocation(location)
    this.opPause()
  }

  private fun opClose() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opClose")
    this.downloadEventSubscription.unsubscribe()
    this.playNothing()
    this.exoPlayer.release()
  }

  override val isPlaying: Boolean
    get() {
      this.checkNotClosed()
      return when (this.stateGet()) {
        is ExoPlayerStateInitial -> false
        is ExoPlayerStatePlaying -> true
        is ExoPlayerStateStopped -> false
        is ExoPlayerStateWaitingForElement -> true
      }
    }

  private fun checkNotClosed() {
    if (this.closed.get()) {
      throw IllegalStateException("Player has been closed")
    }
  }

  override var playbackRate: PlayerPlaybackRate
    get() {
      this.checkNotClosed()
      return this.currentPlaybackRate
    }
    set(value) {
      this.checkNotClosed()
      this.engineExecutor.execute { this.opSetPlaybackRate(value) }
    }

  override val events: Observable<PlayerEvent>
    get() {
      this.checkNotClosed()
      return this.statusEvents
    }

  override fun play() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opPlay() }
  }

  override fun pause() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opPause() }
  }

  override fun skipToNextChapter() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipToNextChapter() }
  }

  override fun skipToPreviousChapter() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipToPreviousChapter() }
  }

  override fun skipForward() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipForward() }
  }

  override fun skipBack() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipBack() }
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opPlayAtLocation(location) }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opMovePlayheadToLocation(location) }
  }

  override val isClosed: Boolean
    get() = this.closed.get()

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.engineExecutor.execute { this.opClose() }
    }
  }
}