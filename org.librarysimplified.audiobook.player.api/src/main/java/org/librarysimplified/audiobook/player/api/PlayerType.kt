package org.librarysimplified.audiobook.player.api

import androidx.media2.common.SessionPlayer

interface PlayerType {

  val sessionPlayer: SessionPlayer

  fun close()
}
