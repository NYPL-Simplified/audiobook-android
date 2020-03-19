package org.librarysimplified.audiobook.manifest.api

/**
 * Extra properties associated with a link.
 */

data class PlayerManifestLinkProperties(
  val extras: Map<String, PlayerManifestScalar> = mapOf(),
  val encrypted: PlayerManifestEncrypted? = null
)
