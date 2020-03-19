package org.librarysimplified.audiobook.manifest_fulfill.basic

import java.net.URI

/**
 * Parameters for fetching manifest from a URI using (optional) basic authentication.
 */

data class ManifestFulfillmentBasicParameters(
  val uri: URI,
  val credentials: ManifestFulfillmentBasicCredentials?
)
