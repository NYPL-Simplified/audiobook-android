package org.librarysimplified.audiobook.license_check.api

/**
 * An interface for composing license checks.
 *
 * A license check generally consists of a set of single license checks. If any of the individual
 * checks fail, the check as a whole is considered to have failed.
 */

interface LicenseCheckProviderType {

  /**
   * Construct a new license check, using checks from the given providers and operating on
   * the given manifest.
   */

  fun createLicenseCheck(
    parameters: LicenseCheckParameters
  ): LicenseCheckType
}
