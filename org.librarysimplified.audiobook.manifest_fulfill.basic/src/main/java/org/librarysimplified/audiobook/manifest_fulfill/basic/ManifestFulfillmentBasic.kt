package org.librarysimplified.audiobook.manifest_fulfill.basic

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentError
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyType
import org.slf4j.LoggerFactory

/**
 * A fulfillment strategy that expects to receive a manifest directly, via HTTP basic authentication.
 */

class ManifestFulfillmentBasic(
  private val client: OkHttpClient,
  private val configuration: ManifestFulfillmentBasicParameters
) : ManifestFulfillmentStrategyType {

  private val logger =
    LoggerFactory.getLogger(ManifestFulfillmentBasic::class.java)

  override fun execute(): PlayerResult<ByteArray, ManifestFulfillmentError> {
    this.logger.debug("fulfilling manifest: {}", this.configuration.uri)

    val credential =
      Credentials.basic(this.configuration.userName, this.configuration.password)

    val request =
      Request.Builder()
        .header("Authorization", credential)
        .url(this.configuration.uri.toURL())
        .build()

    val call = this.client.newCall(request)
    val response = call.execute()
    val bodyData = response.body()?.bytes() ?: ByteArray(0)

    this.logger.debug(
      "received: {} {} for {}",
      response.code(),
      response.message(),
      this.configuration.uri
    )

    if (!response.isSuccessful) {
      return PlayerResult.Failure(
        ManifestFulfillmentError.HTTPRequestFailed(
          code = response.code(),
          message = response.message(),
          receivedBody = bodyData,
          receivedContentType = response.header("Content-Type") ?: "application/octet-stream"
        )
      )
    }

    return PlayerResult.unit(bodyData)
  }
}
