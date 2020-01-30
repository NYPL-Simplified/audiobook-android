package org.librarysimplified.audiobook.feedbooks

import org.joda.time.LocalDateTime
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

class FeedbooksRightsCheck(
  private val manifest: PlayerManifest,
  private val timeNow: LocalDateTime,
  private val onStatusChanged: (SingleLicenseCheckStatus) -> Unit
) : SingleLicenseCheckType {

  override fun execute(): SingleLicenseCheckResult {
    this.event("Started check")

    val rights =
      this.manifest.extensions.find { extension ->
        extension is FeedbooksRights
      } as FeedbooksRights?
        ?: return SingleLicenseCheckResult.NotApplicable("No rights information was provided.")

    if (rights.validStart != null) {
      if (this.timeNow.isBefore(rights.validStart)) {
        return SingleLicenseCheckResult.Failed(
          "The current time precedes the start of the rights date range."
        )
      }
    }

    if (rights.validEnd != null) {
      if (this.timeNow.isAfter(rights.validEnd)) {
        return SingleLicenseCheckResult.Failed(
          "The current time exceeds the end of the rights date range."
        )
      }
    }

    return SingleLicenseCheckResult.Succeeded(
      "The current time is within the specified date range"
    )
  }

  private fun event(message: String) {
    this.onStatusChanged.invoke(
      SingleLicenseCheckStatus(
        source = "FeedbooksRightsCheck",
        message = message
      )
    )
  }
}
