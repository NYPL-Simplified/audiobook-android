package org.librarysimplified.audiobook.feedbooks

import org.joda.time.LocalDateTime
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

class FeedbooksRightsChecks : SingleLicenseCheckProviderType {

  override val name: String =
    "FeedbooksRightsCheck"

  override fun createLicenseCheck(
    manifest: PlayerManifest,
    onStatusChanged: (SingleLicenseCheckStatus) -> Unit
  ): SingleLicenseCheckType {
    return FeedbooksRightsCheck(
      manifest = manifest,
      timeNow = LocalDateTime.now(),
      onStatusChanged = onStatusChanged
    )
  }
}
