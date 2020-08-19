package org.librarysimplified.audiobook.open_access

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import java.net.URI

/**
 * A spine item transformed to expose the information critical to the ExoPlayer engine.
 */

data class ExoManifestSpineItem(
  val title: String?,
  val part: Int,
  val chapter: Int,
  val type: MIMEType,
  val duration: Double?,
  var uri: URI,
  var originalLink: PlayerManifestLink
) {

  /**
   * Update the link in the spine item.
   */

  fun updateLink(
    originalLink: PlayerManifestLink,
    uri: URI
  ) {
    this.originalLink = originalLink
    this.uri = uri
  }
}
