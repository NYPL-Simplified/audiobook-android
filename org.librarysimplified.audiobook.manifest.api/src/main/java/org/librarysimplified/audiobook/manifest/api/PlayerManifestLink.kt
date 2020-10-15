package org.librarysimplified.audiobook.manifest.api

import one.irradia.mime.api.MIMEType
import java.net.URI

/**
 * A link.
 */

sealed class PlayerManifestLink {

  /**
   * The MIME type of the link content.
   */

  abstract val type: MIMEType?

  /**
   * The relation of the link.
   */

  abstract val relation: List<String>

  /**
   * Title of the linked resource
   */

  abstract val title: String?

  /**
   * Height of the linked resource in pixels
   */

  abstract val height: Int?

  /**
   * Width of the linked resource in pixels
   */

  abstract val width: Int?

  /**
   * Duration of the linked resource in seconds
   */

  abstract val duration: Double?

  /**
   * Bit rate of the linked resource in kilobits per second
   */

  abstract val bitrate: Double?

  /**
   * The link target as a URI, if the target is directly expressible as one
   */

  abstract val hrefURI: URI?

  /**
   * Extra properties for the link
   */

  abstract val properties: PlayerManifestLinkProperties

  /**
   * Alternate link targets.
   */

  abstract val alternates: List<PlayerManifestLink>

  /**
   * `true` if the link may expire.
   */

  abstract val expires: Boolean

  /**
   * A non-templated, basic link.
   */

  data class LinkBasic(
    val href: URI?,
    override val type: MIMEType? = null,
    override val relation: List<String> = listOf(),
    override val title: String? = null,
    override val height: Int? = null,
    override val width: Int? = null,
    override val duration: Double? = null,
    override val bitrate: Double? = null,
    override val properties: PlayerManifestLinkProperties = PlayerManifestLinkProperties(),
    override val alternates: List<PlayerManifestLink> = listOf(),
    override val expires: Boolean = false
  ) : PlayerManifestLink() {
    override val hrefURI: URI?
      get() = this.href
  }

  /**
   * A templated link.
   */

  data class LinkTemplated(
    val href: String,
    override val type: MIMEType? = null,
    override val relation: List<String> = listOf(),
    override val title: String? = null,
    override val height: Int? = null,
    override val width: Int? = null,
    override val duration: Double? = null,
    override val bitrate: Double? = null,
    override val properties: PlayerManifestLinkProperties = PlayerManifestLinkProperties(),
    override val alternates: List<PlayerManifestLink> = listOf(),
    override val expires: Boolean = false
  ) : PlayerManifestLink() {
    override val hrefURI: URI?
      get() = null
  }
}
