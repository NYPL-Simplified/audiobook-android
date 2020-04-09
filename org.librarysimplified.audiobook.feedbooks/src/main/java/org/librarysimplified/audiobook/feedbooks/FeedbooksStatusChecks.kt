package org.librarysimplified.audiobook.feedbooks

import okhttp3.OkHttpClient
import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusParsers
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType

class FeedbooksStatusChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksRightsCheck"

  override fun createLicenseCheck(
    parameters: SingleLicenseCheckParameters
  ): SingleLicenseCheckType {
    return FeedbooksStatusCheck(
      httpClient = OkHttpClient(),
      parsers = LicenseStatusParsers,
      parameters = parameters
    )
  }
}
