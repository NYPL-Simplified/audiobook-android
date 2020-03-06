package org.librarysimplified.audiobook.feedbooks

import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType

/**
 * A Feedbooks signature value.
 */

data class FeedbooksSignature(
  val algorithm: String,
  val issuer: String?,
  val value: String
) : PlayerManifestExtensionValueType
