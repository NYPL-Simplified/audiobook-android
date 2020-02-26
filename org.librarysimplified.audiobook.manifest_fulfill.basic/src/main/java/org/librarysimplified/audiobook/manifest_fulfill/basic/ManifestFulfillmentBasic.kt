package org.librarysimplified.audiobook.manifest_fulfill.basic

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentErrorType
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentEvent
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyType
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject

/**
 * A fulfillment strategy that expects to receive a manifest directly, via HTTP basic authentication.
 */

class ManifestFulfillmentBasic(
  private val client: OkHttpClient,
  private val configuration: ManifestFulfillmentBasicParameters
) : ManifestFulfillmentStrategyType {

  private val logger =
    LoggerFactory.getLogger(ManifestFulfillmentBasic::class.java)

  private val eventSubject =
    PublishSubject.create<ManifestFulfillmentEvent>()

  override val events: Observable<ManifestFulfillmentEvent> =
    this.eventSubject

  override fun execute(): PlayerResult<ByteArray, ManifestFulfillmentErrorType> {
    this.logger.debug("fulfilling manifest: {}", this.configuration.uri)

    this.eventSubject.onNext(
      ManifestFulfillmentEvent(
        "Fulfilling ${this.configuration.uri}"
      )
    )

    val credential =
      Credentials.basic(
        this.configuration.userName,
        this.configuration.password
      )

    val request =
      Request.Builder()
        .header("Authorization", credential)
        .url(this.configuration.uri.toURL())
        .build()

    val call = this.client.newCall(request)
    val response = call.execute()
    val bodyData = response.body()?.bytes() ?: ByteArray(0)
    val responseCode = response.code()
    val responseMessage = response.message()

    this.logger.debug(
      "received: {} {} for {}",
      responseCode,
      responseMessage,
      this.configuration.uri
    )

    this.eventSubject.onNext(
      ManifestFulfillmentEvent(
        "Received $responseCode $responseMessage for ${this.configuration.uri}"
      )
    )

    if (!response.isSuccessful) {
      return PlayerResult.Failure(
        HTTPRequestFailed(
          message = responseMessage,
          serverData = ManifestFulfillmentErrorType.ServerData(
            code = responseCode,
            receivedBody = bodyData,
            receivedContentType = response.header("Content-Type") ?: "application/octet-stream"
          )
        )
      )
    }

    return PlayerResult.unit(bodyData)
  }

  override fun close() {
    this.eventSubject.onCompleted()
  }
}
