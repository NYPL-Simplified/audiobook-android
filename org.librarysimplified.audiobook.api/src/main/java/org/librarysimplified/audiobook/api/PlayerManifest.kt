package org.librarysimplified.audiobook.api

/**
 * A raw audio book manifest, parsed and typed.
 */

data class PlayerManifest(
  val readingOrder: List<PlayerManifestLink>,
  val metadata: PlayerManifestMetadata,
  val links: List<PlayerManifestLink>
) {
  @Deprecated(
    message = "Use readingOrder",
    replaceWith = ReplaceWith("readingOrder")
  )
  val spine: List<PlayerManifestLink> =
    this.readingOrder
}
