package org.librarysimplified.audiobook.exoplayer

import androidx.media2.common.SessionPlayer
import com.google.android.exoplayer2.upstream.cache.Cache
import org.librarysimplified.audiobook.player.api.PlayerType

class ExoPlayer(
  override val sessionPlayer: SessionPlayer,
  private val cache: Cache
) : PlayerType {

  override fun close() {
    sessionPlayer.close()
    cache.release()
  }
}
