package org.librarysimplified.audiobook.open_access

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
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventError
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterCompleted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterWaiting
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStarted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.NORMAL_TIME
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadExpired
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateInitial
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStatePlaying
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateStopped
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.ExoPlayerState.ExoPlayerStateWaitingForElement
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_NONEXISTENT
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_NOT_DOWNLOADED
import org.librarysimplified.audiobook.open_access.ExoAudioBookPlayer.SkipChapterStatus.SKIP_TO_CHAPTER_READY
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject
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
  private val statusEvents: BehaviorSubject<PlayerEvent>,
  private val book: ExoAudioBook,
  private val exoPlayer: ExoPlayer,
  manifestUpdates: Observable<Unit>
) : PlayerType {

  private val manifestSubscription: Subscription
  private val log = LoggerFactory.getLogger(ExoAudioBookPlayer::class.java)
  private val bufferSegmentSize = 64 * 1024
  private val bufferSegmentCount = 256
  private val userAgent = "${this.engineProvider.name()}/${this.engineProvider.version()}"
  private val closed = AtomicBoolean(false)

  init {
    this.manifestSubscription = manifestUpdates.subscribe {
      this.onManifestUpdated()
    }
  }

  private fun onManifestUpdated() {
    this.statusEvents.onNext(PlayerEvent.PlayerEventManifestUpdated)
  }

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
      val observerTask: ScheduledFuture<*>
    ) :
      ExoPlayerState()

    /*
     * The player is waiting until the given spine element is downloaded before continuing playback.
     */

    data class ExoPlayerStateWaitingForElement(
      var spineElement: ExoSpineElement
    ) :
      ExoPlayerState()

    /*
     * The player was playing the given spine element but is currently paused.
     */

    data class ExoPlayerStateStopped(
      var spineElement: ExoSpineElement
    ) :
      ExoPlayerState()
  }

  @Volatile
  private var currentPlaybackRate: PlayerPlaybackRate = NORMAL_TIME

  @Volatile
  private var currentPlaybackOffset: Long = 0
    set(value) {
      this.log.trace("currentPlaybackOffset: {}", value)
      field = value
    }

  private val stateLock: Any = Object()

  @GuardedBy("stateLock")
  private var state: ExoPlayerState = ExoPlayerStateInitial

  private val downloadEventSubscription: Subscription
  private val allocator: Allocator = DefaultAllocator(this.bufferSegmentSize)
  private var exoAudioRenderer: MediaCodecAudioTrackRenderer? = null

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
      this@ExoAudioBookPlayer.statusEvents.onNext(
        PlayerEventError(
          spineElement = this@ExoAudioBookPlayer.currentSpineElement(),
          exception = error,
          errorCode = -1,
          offsetMilliseconds = this@ExoAudioBookPlayer.currentPlaybackOffset
        )
      )
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, stateNow: Int) {
      val stateName = this.stateName(stateNow)
      this@ExoAudioBookPlayer.log.debug(
        "onPlayerStateChanged: {} {} ({})", playWhenReady, stateName, stateNow
      )
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
      engineExecutor: ScheduledExecutorService,
      manifestUpdates: Observable<Unit>
    ): ExoAudioBookPlayer {

      val statusEvents =
        BehaviorSubject.create<PlayerEvent>()

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
          statusEvents = statusEvents,
          manifestUpdates = manifestUpdates
        )
      }).get(5L, SECONDS)
    }
  }

  /**
   * A playback observer that is called repeatedly to observe the current state of the player.
   */

  private inner class PlaybackObserver(
    private val spineElement: ExoSpineElement,
    private var initialSeek: Long?
  ) : Runnable {

    private var gracePeriod: Int = 1

    override fun run() {
      val bookPlayer = this@ExoAudioBookPlayer
      val duration = bookPlayer.exoPlayer.duration
      val position = bookPlayer.exoPlayer.currentPosition
      bookPlayer.log.debug("playback: {}/{}", position, duration)
      this.spineElement.duration = Duration.millis(duration)

      /*
       * Perform the initial seek, if necessary.
       */

      val seek = this.initialSeek
      if (seek != null) {
        bookPlayer.engineExecutor.execute {
          if (seek < 0L) {
            bookPlayer.seek(duration + seek)
          } else {
            bookPlayer.seek(seek)
          }
        }
        this.initialSeek = null
      }

      /*
       * Report the current playback status.
       */

      bookPlayer.currentPlaybackOffset = position
      bookPlayer.statusEvents.onNext(
        PlayerEventPlaybackProgressUpdate(this.spineElement, position)
      )

      /*
       * Provide a short grace period before indicating that the current spine element has
       * finished playing. This avoids a situation where the player indicates that it is at
       * the end of the audio but the sound hardware has not actually finished playing the last
       * chunk.
       */

      if (position >= duration) {
        if (this.gracePeriod == 0) {
          bookPlayer.currentPlaybackOffset = bookPlayer.exoPlayer.duration
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
    offset: Long
  ): ScheduledFuture<*> {
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
        0
      )

    this.exoAudioRenderer =
      MediaCodecAudioTrackRenderer(
        sampleSource,
        MediaCodecSelector.DEFAULT,
        null,
        true,
        null,
        null,
        AudioCapabilities.getCapabilities(this.context),
        AudioManager.STREAM_MUSIC
      )

    this.exoPlayer.prepare(this.exoAudioRenderer)
    this.exoPlayer.playWhenReady = true
    this.seek(offset)

    this.setPlayerPlaybackRate(this.currentPlaybackRate)
    return this.schedulePlaybackObserverForSpineElement(spineElement, initialSeek = offset)
  }

  /**
   * Schedule a playback observer that will check the current state of playback once per second.
   */

  private fun schedulePlaybackObserverForSpineElement(
    spineElement: ExoSpineElement,
    initialSeek: Long?
  ): ScheduledFuture<*> {

    return this.engineExecutor.scheduleAtFixedRate(
      this.PlaybackObserver(spineElement, initialSeek), 1L, 1L, SECONDS
    )
  }

  /**
   * The download status of a spine element has changed. We only actually care about
   * the spine element that we're either currently playing or are currently waiting for.
   * Everything else is uninteresting.
   */

  private fun onDownloadStatusChanged(status: PlayerSpineElementDownloadStatus) {
    ExoEngineThread.checkIsExoEngineThread()

    return when (val currentState = this.stateGet()) {
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
            is PlayerSpineElementDownloadExpired,
            is PlayerSpineElementDownloadFailed -> {
              this.log.debug("spine element status changed, stopping playback")
              this.playNothing()
            }
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
            is PlayerSpineElementDownloadExpired,
            is PlayerSpineElementDownloadFailed -> {
              this.log.debug("spine element status changed, stopping playback")
              this.playNothing()
            }
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
            is PlayerSpineElementDownloadExpired,
            is PlayerSpineElementDownloadFailed -> Unit
            is PlayerSpineElementDownloaded -> {
              this.log.debug("spine element status changed, trying to start playback")
              this.playSpineElementIfAvailable(currentState.spineElement, 0)
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
    this.log.debug("setPlayerPlaybackRate: {}", newRate)

    this.statusEvents.onNext(PlayerEventPlaybackRateChanged(newRate))

    /*
     * If the player has not started playing a track, then attempting to set the playback
     * rate on the player will actually end up blocking until another track is loaded.
     */

    if (this.exoAudioRenderer != null) {
      if (Build.VERSION.SDK_INT >= 23) {
        val params = PlaybackParams()
        params.speed = newRate.speed.toFloat()
        this.exoPlayer.sendMessage(this.exoAudioRenderer, 2, params)
      }
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

    return when (val currentState = this.stateGet()) {
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

  private fun playFirstSpineElementIfAvailable(offset: Long): SkipChapterStatus {
    this.log.debug("playFirstSpineElementIfAvailable: {}", offset)

    val firstElement = this.book.spine.firstOrNull()
    if (firstElement == null) {
      this.log.debug("no available initial spine element")
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return this.playSpineElementIfAvailable(firstElement, offset)
  }

  private fun playLastSpineElementIfAvailable(offset: Long): SkipChapterStatus {
    this.log.debug("playLastSpineElementIfAvailable: {}", offset)

    val lastElement = this.book.spine.lastOrNull()
    if (lastElement == null) {
      this.log.debug("no available final spine element")
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return this.playSpineElementIfAvailable(lastElement, offset)
  }

  private fun playNextSpineElementIfAvailable(
    element: ExoSpineElement,
    offset: Long
  ): SkipChapterStatus {
    this.log.debug("playNextSpineElementIfAvailable: {} {}", element.index, offset)

    val next = element.next as ExoSpineElement?
    if (next == null) {
      this.log.debug("spine element {} has no next element", element.index)
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return this.playSpineElementIfAvailable(next, offset)
  }

  private fun playPreviousSpineElementIfAvailable(
    element: ExoSpineElement,
    offset: Long
  ): SkipChapterStatus {
    this.log.debug("playPreviousSpineElementIfAvailable: {} {}", element.index, offset)

    val previous = element.previous as ExoSpineElement?
    if (previous == null) {
      this.log.debug("spine element {} has no previous element", element.index)
      return SKIP_TO_CHAPTER_NONEXISTENT
    }

    return this.playSpineElementIfAvailable(previous, offset)
  }

  private fun playSpineElementIfAvailable(
    element: ExoSpineElement,
    offset: Long
  ): SkipChapterStatus {
    this.log.debug("playSpineElementIfAvailable: {}", element.index)
    this.playNothing()

    return when (val downloadStatus = element.downloadStatus) {
      is PlayerSpineElementNotDownloaded,
      is PlayerSpineElementDownloading,
      is PlayerSpineElementDownloadExpired,
      is PlayerSpineElementDownloadFailed -> {
        this.log.debug(
          "playSpineElementIfAvailable: spine element {} is not downloaded ({}), cannot continue",
          element.index, downloadStatus
        )

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

  private fun playSpineElementUnconditionally(element: ExoSpineElement, offset: Long = 0) {
    this.log.debug("playSpineElementUnconditionally: {}", element.index)

    this.stateSet(
      ExoPlayerStatePlaying(
        spineElement = element,
        observerTask = this.openSpineElement(element, offset)
      )
    )
    this.statusEvents.onNext(PlayerEventPlaybackStarted(element, offset))
    this.currentPlaybackOffset = offset
  }

  private fun seek(offsetMs: Long) {
    this.log.debug("seek: {}", offsetMs)
    this.exoPlayer.seekTo(offsetMs)
    this.currentPlaybackOffset = offsetMs
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

    return when (val state = this.stateGet()) {
      is ExoPlayerStateInitial -> {
        this.playFirstSpineElementIfAvailable(offset = 0)
        Unit
      }

      is ExoPlayerStatePlaying ->
        this.log.debug("opPlay: already playing")

      is ExoPlayerStateStopped ->
        this.opPlayStopped(state)

      is ExoPlayerStateWaitingForElement -> {
        this.playSpineElementIfAvailable(state.spineElement, offset = 0)
        Unit
      }
    }
  }

  private fun opPlayStopped(state: ExoPlayerStateStopped) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayStopped")

    this.exoPlayer.playWhenReady = true

    this.stateSet(
      ExoPlayerStatePlaying(
        spineElement = state.spineElement,
        observerTask = this.schedulePlaybackObserverForSpineElement(
          spineElement = state.spineElement,
          initialSeek = null
        )
      )
    )

    this.statusEvents.onNext(
      PlayerEventPlaybackStarted(
        state.spineElement, this.currentPlaybackOffset
      )
    )
  }

  private fun opCurrentTrackFinished() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opCurrentTrackFinished")

    return when (val state = this.stateGet()) {
      is ExoPlayerStateInitial,
      is ExoPlayerStateWaitingForElement,
      is ExoPlayerStateStopped -> {
        this.log.error("current track is finished but the player thinks it is not playing!")
        throw Unimplemented()
      }

      is ExoPlayerStatePlaying -> {
        this.statusEvents.onNext(PlayerEventChapterCompleted(state.spineElement))

        when (this.playNextSpineElementIfAvailable(state.spineElement, offset = 0)) {
          SKIP_TO_CHAPTER_NOT_DOWNLOADED,
          SKIP_TO_CHAPTER_READY -> Unit
          SKIP_TO_CHAPTER_NONEXISTENT ->
            this.playNothing()
        }
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

  private fun opSkipToNextChapter(offset: Long): SkipChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToNextChapter")

    return when (val state = this.stateGet()) {
      is ExoPlayerStateInitial ->
        this.playFirstSpineElementIfAvailable(offset)
      is ExoPlayerStatePlaying ->
        this.playNextSpineElementIfAvailable(state.spineElement, offset)
      is ExoPlayerStateStopped ->
        this.playNextSpineElementIfAvailable(state.spineElement, offset)
      is ExoPlayerStateWaitingForElement ->
        this.playNextSpineElementIfAvailable(state.spineElement, offset)
    }
  }

  private fun opSkipToPreviousChapter(offset: Long): SkipChapterStatus {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipToPreviousChapter")

    return when (val state = this.stateGet()) {
      ExoPlayerStateInitial ->
        this.playLastSpineElementIfAvailable(offset)
      is ExoPlayerStatePlaying ->
        this.playPreviousSpineElementIfAvailable(state.spineElement, offset)
      is ExoPlayerStateWaitingForElement ->
        this.playPreviousSpineElementIfAvailable(state.spineElement, offset)
      is ExoPlayerStateStopped ->
        this.playPreviousSpineElementIfAvailable(state.spineElement, offset)
    }
  }

  private fun opPause() {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPause")

    return when (val state = this.stateGet()) {
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
    this.log.debug("opPausePlaying: offset: {}", this.currentPlaybackOffset)

    state.observerTask.cancel(true)
    this.exoPlayer.playWhenReady = false

    this.stateSet(ExoPlayerStateStopped(spineElement = state.spineElement))
    this.statusEvents.onNext(
      PlayerEventPlaybackPaused(
        state.spineElement, this.currentPlaybackOffset
      )
    )
  }

  private fun opSkipPlayhead(milliseconds: Long) {
    this.log.debug("opSkipPlayhead")
    return when {
      milliseconds == 0L -> {
      }
      milliseconds > 0 -> opSkipForward(milliseconds)
      else -> opSkipBack(milliseconds)
    }
  }

  private fun opSkipForward(milliseconds: Long) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipForward")

    assert(milliseconds > 0, { "Milliseconds must be positive" })

    val offset =
      Math.min(this.exoPlayer.duration, this.exoPlayer.currentPosition + milliseconds)
    this.seek(offset)

    return when (val state = this.stateGet()) {
      ExoPlayerStateInitial,
      is ExoPlayerStatePlaying,
      is ExoPlayerStateWaitingForElement -> Unit
      is ExoPlayerStateStopped ->
        this.statusEvents.onNext(
          PlayerEventPlaybackPaused(
            state.spineElement, this.currentPlaybackOffset
          )
        )
    }
  }

  private fun opSkipBack(milliseconds: Long) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opSkipBack")

    assert(milliseconds < 0, { "Milliseconds must be negative" })

    /*
     * If the current time is in the range [00:00, 00:04], skipping back should switch
     * to the previous spine element and then jump 15 seconds back from the end of that
     * element. Otherwise, it should simply skip backwards, clamping the minimum to 00:00.
     */

    val current = this.exoPlayer.currentPosition
    if (current <= 4_000L) {
      this.opSkipToPreviousChapter(milliseconds)
    } else {
      this.seek(Math.max(0L, current + milliseconds))
    }

    return when (val state = this.stateGet()) {
      ExoPlayerStateInitial,
      is ExoPlayerStatePlaying,
      is ExoPlayerStateWaitingForElement -> Unit
      is ExoPlayerStateStopped ->
        this.statusEvents.onNext(
          PlayerEventPlaybackPaused(
            state.spineElement, this.currentPlaybackOffset
          )
        )
    }
  }

  private fun opPlayAtLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
    this.log.debug("opPlayAtLocation: {}", location)

    val currentSpineElement =
      this.currentSpineElement()

    val requestedSpineElement =
      this.book.spineElementForPartAndChapter(location.part, location.chapter)
        as ExoSpineElement?

    if (requestedSpineElement == null) {
      return this.log.debug("spine element does not exist")
    }

    /*
     * If the current spine element is the same as the requested spine element, then it's more
     * efficient to simply seek to the right offset and start playing.
     */

    if (requestedSpineElement == currentSpineElement) {
      this.seek(location.offsetMilliseconds)
      this.opPlay()
    } else {
      this.playSpineElementIfAvailable(requestedSpineElement, location.offsetMilliseconds)
    }
  }

  private fun currentSpineElement(): ExoSpineElement? {
    return when (val state = this.stateGet()) {
      ExoPlayerStateInitial -> null
      is ExoPlayerStatePlaying -> state.spineElement
      is ExoPlayerStateWaitingForElement -> null
      is ExoPlayerStateStopped -> state.spineElement
    }
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
    this.manifestSubscription.unsubscribe()
    this.downloadEventSubscription.unsubscribe()
    this.playNothing()
    this.exoPlayer.release()
    this.statusEvents.onCompleted()
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
    this.engineExecutor.execute { this.opSkipToNextChapter(offset = 0) }
  }

  override fun skipToPreviousChapter() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipToPreviousChapter(offset = 0) }
  }

  override fun skipPlayhead(milliseconds: Long) {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opSkipPlayhead(milliseconds) }
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opPlayAtLocation(location) }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opMovePlayheadToLocation(location) }
  }

  override fun playAtBookStart() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opPlayAtLocation(this.book.spine.first().position) }
  }

  override fun movePlayheadToBookStart() {
    this.checkNotClosed()
    this.engineExecutor.execute { this.opMovePlayheadToLocation(this.book.spine.first().position) }
  }

  override val isClosed: Boolean
    get() = this.closed.get()

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.engineExecutor.execute { this.opClose() }
    }
  }
}
