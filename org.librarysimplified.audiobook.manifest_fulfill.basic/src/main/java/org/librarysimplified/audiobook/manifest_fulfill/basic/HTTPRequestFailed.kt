package org.librarysimplified.audiobook.manifest_fulfill.basic

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentErrorType

/**
 * A generic error that indicates that an HTTP server request failed.
 */

data class HTTPRequestFailed(
  override val message: String,
  override val serverData: ManifestFulfillmentErrorType.ServerData
) : ManifestFulfillmentErrorType
