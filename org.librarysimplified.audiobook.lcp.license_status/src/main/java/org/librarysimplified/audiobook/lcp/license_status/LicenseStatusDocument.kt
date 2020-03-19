package org.librarysimplified.audiobook.lcp.license_status

import java.util.Locale

/**
 * An LCP license status document.
 *
 * @see "https://readium.org/lcp-specs/releases/lsd/latest.html"
 */

data class LicenseStatusDocument(
  val status: Status
) {

  /**
   * The status of a license.
   */

  enum class Status {

    /**
     * The License Document is available, but the user hasn’t accessed the License and/or Status Document yet.
     */

    READY,

    /**
     * The license is active, and a device has been successfully registered for this license.
     * This is the default value if the License Document does not contain a registration link, or a
     * registration mechanism through the license itself.
     */

    ACTIVE,

    /**
     * The license is no longer active, it has been invalidated by the Issuer.
     */

    REVOKED,

    /**
     * The license is no longer active, it has been invalidated by the User.
     */

    RETURNED,

    /**
     * The license is no longer active because it was cancelled prior to activation.
     */

    CANCELLED,

    /**
     *  The license is no longer active because it has expired.
     */

    EXPIRED;

    override fun toString(): String =
      super.name.toLowerCase(Locale.ROOT)

    companion object {

      fun ofString(text: String): Status? =
        try {
          valueOf(text.toUpperCase(Locale.ROOT))
        } catch (e: Exception) {
          null
        }
    }
  }
}
