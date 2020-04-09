package org.librarysimplified.audiobook.api

/**
 * The credentials that can be used for downloads.
 */

sealed class PlayerDownloadRequestCredentials {

  /**
   * Credentials for basic HTTP auth.
   */

  data class Basic(
    val user: String,
    val password: String
  ) : PlayerDownloadRequestCredentials()

  /**
   * Credentials for bearer token HTTP auth.
   */

  data class BearerToken(
    val token: String
  ) : PlayerDownloadRequestCredentials()
}
