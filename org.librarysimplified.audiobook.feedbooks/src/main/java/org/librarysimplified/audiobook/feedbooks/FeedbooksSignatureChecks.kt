package org.librarysimplified.audiobook.feedbooks

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType

class FeedbooksSignatureChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksSignatureCheck"

  override fun createLicenseCheck(
    parameters: SingleLicenseCheckParameters
  ): SingleLicenseCheckType {
    return FeedbooksSignatureCheck(parameters)
  }
}
