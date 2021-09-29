package org.librarysimplified.audiobook.manifest_fulfill.basic

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import one.irradia.mime.vanilla.MIMEParser
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfilled
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

  override fun execute(): PlayerResult<ManifestFulfilled, ManifestFulfillmentErrorType> {
    this.logger.debug("fulfilling manifest: {}", this.configuration.uri)

    this.eventSubject.onNext(
      ManifestFulfillmentEvent(
        "Fulfilling ${this.configuration.uri}"
      )
    )

    val requestBuilder = Request.Builder()
    val credentials = this.configuration.credentials
    if (credentials != null) {
      val credential =
        Credentials.basic(credentials.userName, credentials.password)
      requestBuilder.header("Authorization", credential)
    }

    val request =
      requestBuilder
        .url(this.configuration.uri.toURL())
        .header("User-Agent", this.configuration.userAgent.userAgent)
        .build()

    val call = this.client.newCall(request)
    val response = call.execute()
    val bodyData = response.body?.bytes() ?: ByteArray(0)
    val responseCode = response.code
    val responseMessage = response.message
    val contentType = response.header("Content-Type") ?: "application/octet-stream"

    this.logger.debug(
      "received: {} {} for {} ({})",
      responseCode,
      responseMessage,
      this.configuration.uri,
      contentType
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
            uri = this.configuration.uri,
            code = responseCode,
            receivedBody = bodyData,
            receivedContentType = contentType
          )
        )
      )
    }

    return PlayerResult.unit(
      ManifestFulfilled(
        contentType = MIMEParser.parseRaisingException(contentType),
        data = bodyData
      )
    )
  }

  override fun close() {
    this.eventSubject.onCompleted()
  }
}
