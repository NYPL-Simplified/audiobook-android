package org.librarysimplified.audiobook.manifest_fulfill.spi

sealed class ManifestFulfillmentError {

  data class HTTPRequestFailed(
    val code: Int,
    val message: String,
    val receivedBody: ByteArray,
    val receivedContentType: String
  ) : ManifestFulfillmentError()
}
