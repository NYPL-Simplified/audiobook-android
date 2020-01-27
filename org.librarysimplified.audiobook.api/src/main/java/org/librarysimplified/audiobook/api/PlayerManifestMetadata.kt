package org.librarysimplified.audiobook.api

/**
 * The metadata section in a manifest.
 */

data class PlayerManifestMetadata(
  val title: String,
  val identifier: String,
  val encrypted: PlayerManifestEncrypted?
)