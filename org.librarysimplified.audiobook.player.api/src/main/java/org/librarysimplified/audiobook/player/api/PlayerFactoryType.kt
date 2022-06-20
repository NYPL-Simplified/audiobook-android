package org.librarysimplified.audiobook.player.api

interface PlayerFactoryType {

  fun createPlayer(): PlayerType

  fun createDownloadManager(): PlayerDownloadManagerType
}
