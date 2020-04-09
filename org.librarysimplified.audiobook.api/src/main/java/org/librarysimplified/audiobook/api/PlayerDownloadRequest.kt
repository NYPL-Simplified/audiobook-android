package org.librarysimplified.audiobook.api

import java.io.File
import java.net.URI

/**
 * A request to download data from a given URI.
 */

data class PlayerDownloadRequest(
  val uri: URI,
  val userAgent: PlayerUserAgent,
  val outputFile: File,
  val credentials: PlayerDownloadRequestCredentials?,
  val onProgress: (Int) -> Unit
)
