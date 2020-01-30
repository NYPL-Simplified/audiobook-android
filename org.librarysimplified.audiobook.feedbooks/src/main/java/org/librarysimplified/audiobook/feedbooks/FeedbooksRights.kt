package org.librarysimplified.audiobook.feedbooks

import org.joda.time.LocalDateTime
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType

/**
 * A Feedbooks rights value.
 */

data class FeedbooksRights(
  val validStart: LocalDateTime?,
  val validEnd: LocalDateTime?
): PlayerManifestExtensionValueType
