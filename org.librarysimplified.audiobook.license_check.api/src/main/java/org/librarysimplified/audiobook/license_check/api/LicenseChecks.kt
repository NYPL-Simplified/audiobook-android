package org.librarysimplified.audiobook.license_check.api

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

/**
 * The default API for performing license checks.
 */

object LicenseChecks : LicenseCheckProviderType {

  override fun createLicenseCheck(
    manifest: PlayerManifest,
    checks: List<SingleLicenseCheckProviderType>
  ): LicenseCheckType {
    return LicenseCheck(
      manifest = manifest,
      checks = checks
    )
  }
}
