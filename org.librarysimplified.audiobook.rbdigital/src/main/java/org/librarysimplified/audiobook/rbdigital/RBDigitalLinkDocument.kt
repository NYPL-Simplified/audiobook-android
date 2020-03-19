package org.librarysimplified.audiobook.rbdigital

import java.net.URI

/**
 * An indirect link to another file.
 */

data class RBDigitalLinkDocument(

  /**
   * The MIME type of the target.
   */

  val type: String,

  /**
   * The URI of the target.
   */

  val uri: URI
)
