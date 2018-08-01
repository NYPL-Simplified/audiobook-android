package org.nypl.audiobook.android.open_access

import android.content.Context
import com.google.android.exoplayer.ExoPlayer
import net.jcip.annotations.GuardedBy
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerType
import rx.Observable
import rx.subjects.PublishSubject
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * An ExoPlayer player.
 */

class ExoAudioBookPlayer private constructor(
  private val engineExecutor: ExecutorService,
  private val statusEvents: PublishSubject<PlayerEvent>,
  private val exoPlayer: ExoPlayer)
  : PlayerType {

  private data class State(
    var playing: Boolean,
    var rate: PlayerPlaybackRate)

  private val stateLock: Any = Object()
  @GuardedBy("stateLock")
  private var state: State =
    State(
      playing = true,
      rate = PlayerPlaybackRate.NORMAL_TIME)

  companion object {

    fun create(
      context: Context,
      engineExecutor: ExecutorService,
      id: PlayerBookID,
      directory: File): ExoAudioBookPlayer {

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

        val player = ExoPlayer.Factory.newInstance(1)
        ExoAudioBookPlayer(
          engineExecutor = engineExecutor,
          exoPlayer = player,
          statusEvents = statusEvents)

      }).get(5L, TimeUnit.SECONDS)
    }
  }

  private fun opSetPlaybackRate(value: PlayerPlaybackRate) {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opPlay() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opSkipToNextChapter() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opPause() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opSkipToPreviousChapter() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opSkipForward() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opSkipBack() {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opPlayAtLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
  }

  private fun opMovePlayheadToLocation(location: PlayerPosition) {
    ExoEngineThread.checkIsExoEngineThread()
  }

  override val isPlaying: Boolean
    get() = synchronized(this.stateLock, { this.state.playing })

  override var playbackRate: PlayerPlaybackRate
    get() = synchronized(this.stateLock, { this.state.rate })
    set(value) {
      this.engineExecutor.submit { this.opSetPlaybackRate(value) }
    }

  override val events: Observable<PlayerEvent>
    get() = this.statusEvents

  override fun play() {
    this.engineExecutor.submit { this.opPlay() }
  }

  override fun pause() {
    this.engineExecutor.submit { this.opPause() }
  }

  override fun skipToNextChapter() {
    this.engineExecutor.submit { this.opSkipToNextChapter() }
  }

  override fun skipToPreviousChapter() {
    this.engineExecutor.submit { this.opSkipToPreviousChapter() }
  }

  override fun skipForward() {
    this.engineExecutor.submit { this.opSkipForward() }
  }

  override fun skipBack() {
    this.engineExecutor.submit { this.opSkipBack() }
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.engineExecutor.submit { this.opPlayAtLocation(location) }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.engineExecutor.submit { this.opMovePlayheadToLocation(location) }
  }
}