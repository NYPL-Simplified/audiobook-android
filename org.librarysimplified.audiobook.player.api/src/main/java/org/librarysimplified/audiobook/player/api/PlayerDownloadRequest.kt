package org.librarysimplified.audiobook.player.api

import java.io.File
import java.net.URI

/**
 * A request to download data from a given URI.
 */

data class PlayerDownloadRequest(
  val uri: URI,
  val userAgent: PlayerUserAgent,
  val outputFile: File,
  val onProgress: (Int) -> Unit
)
