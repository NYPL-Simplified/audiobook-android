package org.librarysimplified.audiobook.license_check.spi

/**
 * The status of a single license check.
 */

data class SingleLicenseCheckStatus(
  val source: String,
  val message: String
)
