package org.librarysimplified.audiobook.manifest_fulfill.spi

import one.irradia.mime.api.MIMEType

/**
 * A downloaded manifest.
 */

data class ManifestFulfilled(
  val contentType: MIMEType,
  val data: ByteArray
)
