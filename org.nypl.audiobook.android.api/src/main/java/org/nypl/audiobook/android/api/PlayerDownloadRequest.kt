package org.nypl.audiobook.android.api

import java.io.File
import java.net.URI

/**
 * A request to download data from a given URI.
 */

data class PlayerDownloadRequest(
  val uri: URI,
  val outputFile: File,
  val credentials: PlayerDownloadRequestCredentials?,
  val onProgress: (Int) -> Unit)

/**
 * The credentials that can be used for downloads.
 */

sealed class PlayerDownloadRequestCredentials {

  /**
   * Credentials for basic HTTP auth.
   */

  data class PlayerDownloadRequestCredentialsBasic(
    val user: String,
    val password: String) : PlayerDownloadRequestCredentials()

}


