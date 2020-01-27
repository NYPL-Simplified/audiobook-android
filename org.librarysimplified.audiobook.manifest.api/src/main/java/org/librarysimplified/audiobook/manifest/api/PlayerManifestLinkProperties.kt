package org.librarysimplified.audiobook.manifest.api

/**
 * Extra properties associated with a link.
 */

data class PlayerManifestLinkProperties(
  val encrypted: PlayerManifestEncrypted? = null
)
