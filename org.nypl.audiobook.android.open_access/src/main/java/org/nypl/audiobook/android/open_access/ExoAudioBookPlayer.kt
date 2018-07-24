package org.nypl.audiobook.android.open_access

import android.content.Context
import com.google.android.exoplayer.ExoPlayer
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory
import rx.Observable
import java.io.File

/**
 * An ExoPlayer player.
 */

class ExoAudioBookPlayer private constructor(
  private val exoPlayer: ExoPlayer)
  : PlayerType {

  companion object {

    private val log = LoggerFactory.getLogger(ExoAudioBookPlayer::class.java)

    fun create(context: Context, id : PlayerBookID): ExoAudioBookPlayer {

      val directory = findDirectoryFor(context, id)
      this.log.debug("book directory: {}", directory)

      /*
       * The rendererCount parameter is not well documented. It appears to be the number of
       * renderers that are required to render a single track. To render a piece of video that
       * had video, audio, and a subtitle track would require three renderers. Audio books should
       * require just one.
       */

      val player = ExoPlayer.Factory.newInstance(1)
      return ExoAudioBookPlayer(player)
    }

    private fun findDirectoryFor(context: Context, id: PlayerBookID): File {
      val base = context.filesDir
      val all = File(base, "exoplayer_audio")
      return File(all, id.value)
    }
  }

  override val isPlaying: Boolean
    get() = TODO("not implemented")

  override var playbackRate: PlayerPlaybackRate
    get() = TODO("not implemented")
    set(value) {}

  override val events: Observable<PlayerEvent>
    get() = TODO("not implemented")

  override fun play() {
    TODO("not implemented")
  }

  override fun pause() {
    TODO("not implemented")
  }

  override fun skipToNextChapter() {
    TODO("not implemented")
  }

  override fun skipToPreviousChapter() {
    TODO("not implemented")
  }

  override fun skipForward() {
    TODO("not implemented")
  }

  override fun skipBack() {
    TODO("not implemented")
  }

  override fun playAtLocation(location: PlayerPosition) {
    TODO("not implemented")
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    TODO("not implemented")
  }
}