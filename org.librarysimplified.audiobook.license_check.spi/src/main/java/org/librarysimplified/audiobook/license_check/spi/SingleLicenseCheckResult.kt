package org.librarysimplified.audiobook.license_check.spi

/**
 * The result of executing a single license check.
 */

sealed class SingleLicenseCheckResult {

  /**
   * The message returned as a result of performing a check.
   */

  abstract val message: String

  /**
   * The short, humanly-readable name for the result.
   */

  abstract val shortName: String

  /**
   * The license check succeeded.
   */

  data class Succeeded(
    override val message: String
  ) : SingleLicenseCheckResult() {
    override val shortName: String =
      "Succeeded"
  }

  /**
   * The license check is not applicable to this manifest.
   */

  data class NotApplicable(
    override val message: String
  ) : SingleLicenseCheckResult() {
    override val shortName: String =
      "Not applicable"
  }

  /**
   * The license check failed.
   */

  data class Failed(
    override val message: String,
    val exception: Exception?
  ) : SingleLicenseCheckResult() {
    override val shortName: String =
      "Failed"
  }
}
