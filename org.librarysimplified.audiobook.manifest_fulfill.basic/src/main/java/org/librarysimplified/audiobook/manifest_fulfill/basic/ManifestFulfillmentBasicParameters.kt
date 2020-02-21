package org.librarysimplified.audiobook.manifest_fulfill.basic

import java.net.URI

/**
 * Parameters for fetching manifest from a URI using basic authentication.
 */

data class ManifestFulfillmentBasicParameters(
  val uri: URI,
  val userName: String,
  val password: String
)
