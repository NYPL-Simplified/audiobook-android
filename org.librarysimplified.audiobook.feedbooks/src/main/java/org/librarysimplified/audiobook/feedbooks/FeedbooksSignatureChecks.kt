package org.librarysimplified.audiobook.feedbooks

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

class FeedbooksSignatureChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksSignatureCheck"

  override fun createLicenseCheck(
    manifest: PlayerManifest,
    onStatusChanged: (SingleLicenseCheckStatus) -> Unit
  ): SingleLicenseCheckType {
    return FeedbooksSignatureCheck(
      manifest = manifest,
      onStatusChanged = onStatusChanged
    )
  }
}
