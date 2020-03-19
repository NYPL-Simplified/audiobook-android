package org.librarysimplified.audiobook.license_check.spi

/**
 * A single license check.
 */

interface SingleLicenseCheckType {

  /**
   * Execute the license check.
   */

  fun execute(): SingleLicenseCheckResult
}
