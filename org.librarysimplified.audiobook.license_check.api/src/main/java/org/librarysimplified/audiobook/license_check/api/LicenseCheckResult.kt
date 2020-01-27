package org.librarysimplified.audiobook.license_check.api

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult

/**
 * The result of performing license checking.
 *
 * Checking licensing generally consists of a series of individual checks. Any individual
 * check failing results in failing of the license check as a whole.
 */

data class LicenseCheckResult(
  val checkStatuses: List<SingleLicenseCheckResult>
) {

  /**
   * License checking succeeded if none of the individual checks failed.
   */

  fun checkSucceeded(): Boolean {
    return !this.checkStatuses.any { status ->
      status is SingleLicenseCheckResult.Failed
    }
  }
}
