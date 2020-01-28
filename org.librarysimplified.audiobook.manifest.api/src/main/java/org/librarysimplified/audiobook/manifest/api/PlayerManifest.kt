package org.librarysimplified.audiobook.manifest.api

/**
 * A raw audio book manifest, parsed and typed.
 */

data class PlayerManifest(
  val originalBytes: ByteArray,
  val readingOrder: List<PlayerManifestLink>,
  val metadata: PlayerManifestMetadata,
  val links: List<PlayerManifestLink>,
  val extensions: List<PlayerManifestExtensionValueType>
) {
  @Deprecated(
    message = "Use readingOrder",
    replaceWith = ReplaceWith("readingOrder")
  )
  val spine: List<PlayerManifestLink> =
    this.readingOrder
}
