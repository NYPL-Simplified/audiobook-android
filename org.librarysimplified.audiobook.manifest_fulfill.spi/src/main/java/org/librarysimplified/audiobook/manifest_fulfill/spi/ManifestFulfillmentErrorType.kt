package org.librarysimplified.audiobook.manifest_fulfill.spi

import java.net.URI

/**
 * The type of errors encountered during manifest fulfillment.
 */

interface ManifestFulfillmentErrorType {

  /**
   * The error message associated with the error.
   */

  val message: String

  /**
   * The server data associated with the error, if any
   */

  val serverData: ServerData?

  data class ServerData(
    val uri: URI,
    val code: Int,
    val receivedBody: ByteArray,
    val receivedContentType: String
  )
}
