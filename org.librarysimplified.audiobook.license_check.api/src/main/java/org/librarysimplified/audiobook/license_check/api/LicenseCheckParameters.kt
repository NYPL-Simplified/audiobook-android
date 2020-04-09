package org.librarysimplified.audiobook.license_check.api

import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

/**
 * The parameters for a set of license checks.
 */

data class LicenseCheckParameters(

  /**
   * The manifest upon which the license checks will be evaluated.
   */

  val manifest: PlayerManifest,

  /**
   * The user agent used in any HTTP requests.
   */

  val userAgent: PlayerUserAgent,

  /**
   * The list of license checks that will be evaluated.
   */

  val checks: List<SingleLicenseCheckProviderType>
)
