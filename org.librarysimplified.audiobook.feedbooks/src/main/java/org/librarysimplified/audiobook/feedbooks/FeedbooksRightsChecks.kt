package org.librarysimplified.audiobook.feedbooks

import org.joda.time.LocalDateTime
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType

class FeedbooksRightsChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksRightsCheck"

  override fun createLicenseCheck(
    parameters: SingleLicenseCheckParameters
  ): SingleLicenseCheckType {
    return FeedbooksRightsCheck(
      parameters = parameters,
      timeNow = LocalDateTime.now()
    )
  }
}
