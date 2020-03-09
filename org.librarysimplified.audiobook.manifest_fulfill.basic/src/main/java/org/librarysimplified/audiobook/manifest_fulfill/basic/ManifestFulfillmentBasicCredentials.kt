package org.librarysimplified.audiobook.manifest_fulfill.basic

/**
 * Parameters for fetching manifest from a URI using basic authentication.
 */

data class ManifestFulfillmentBasicCredentials(
  val userName: String,
  val password: String
)
