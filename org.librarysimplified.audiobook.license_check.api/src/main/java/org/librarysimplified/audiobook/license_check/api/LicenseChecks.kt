package org.librarysimplified.audiobook.license_check.api

/**
 * The default API for performing license checks.
 */

object LicenseChecks : LicenseCheckProviderType {

  override fun createLicenseCheck(
    parameters: LicenseCheckParameters
  ): LicenseCheckType {
    return LicenseCheck(parameters)
  }
}
