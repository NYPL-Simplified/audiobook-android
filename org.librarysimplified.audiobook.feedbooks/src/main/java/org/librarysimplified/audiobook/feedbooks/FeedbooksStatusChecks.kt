package org.librarysimplified.audiobook.feedbooks

import okhttp3.OkHttpClient
import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusParsers
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

class FeedbooksStatusChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksRightsCheck"

  override fun createLicenseCheck(
    manifest: PlayerManifest,
    onStatusChanged: (SingleLicenseCheckStatus) -> Unit
  ): SingleLicenseCheckType {
    return FeedbooksStatusCheck(
      httpClient = OkHttpClient(),
      parsers = LicenseStatusParsers,
      manifest = manifest,
      onStatusChanged = onStatusChanged
    )
  }
}
