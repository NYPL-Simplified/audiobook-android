package org.librarysimplified.audiobook.feedbooks

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.PlayerDownloadRequestCredentials.BearerToken
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.json_web_token.JOSEHeader
import org.librarysimplified.audiobook.json_web_token.JSONWebSignature
import org.librarysimplified.audiobook.json_web_token.JSONWebSignatureAlgorithmHMACSha256
import org.librarysimplified.audiobook.json_web_token.JSONWebTokenClaims
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ExecutorService

/**
 * A player extension for Feedbooks audio books.
 */

class FeedbooksPlayerExtension : PlayerExtensionType {

  /**
   * The configuration data required for operation. If no configuration information
   * is provided, the extension is disabled.
   */

  @Volatile
  var configuration: FeedbooksPlayerExtensionConfiguration? = null

  private val logger =
    LoggerFactory.getLogger(FeedbooksPlayerExtension::class.java)

  override val name: String =
    "org.librarysimplified.audiobook.feedbooks"

  override fun onDownloadLink(
    statusExecutor: ExecutorService,
    downloadProvider: PlayerDownloadProviderType,
    originalRequest: PlayerDownloadRequest,
    link: PlayerManifestLink
  ): ListenableFuture<Unit>? {

    return when (link.properties.encrypted?.scheme) {
      "http://www.feedbooks.com/audiobooks/access-restriction" ->
        this.runBearerTokenRetrieval(
          downloadProvider = downloadProvider,
          originalRequest = originalRequest
        )
      else ->
        null
    }
  }

  private fun runBearerTokenRetrieval(
    downloadProvider: PlayerDownloadProviderType,
    originalRequest: PlayerDownloadRequest
  ): ListenableFuture<Unit> {

    this.logger.debug("running bearer token authentication for {}", originalRequest.uri)

    val currentConfiguration =
      this.configuration ?: return Futures.immediateFailedFuture(
        IllegalStateException(
          "Link requires Feedbooks support, but the Feedbooks extension has not been configured."
        )
      )

    val tokenHeader =
      JOSEHeader(
        mapOf(
          Pair("alg", "HS256"),
          Pair("typ", "JWT")
        )
      )

    val tokenClaims =
      JSONWebTokenClaims(
        mapOf(
          Pair("iss", currentConfiguration.issuerURL),
          Pair("sub", originalRequest.uri.toString()),
          Pair("jti", UUID.randomUUID().toString())
        )
      )

    val token =
      JSONWebSignature.create(
        algorithm = JSONWebSignatureAlgorithmHMACSha256.withSecret(
          currentConfiguration.bearerTokenSecret
        ),
        header = tokenHeader,
        payload = tokenClaims
      )

    val newRequest =
      originalRequest.copy(credentials = BearerToken(token.encode()))

    return downloadProvider.download(newRequest)
  }
}
