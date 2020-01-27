package org.librarysimplified.audiobook.api

/**
 * A section in a manifest dealing with encryption details.
 */

data class PlayerManifestEncrypted(
  val scheme: String,
  val values: Map<String, PlayerManifestScalar>
)
