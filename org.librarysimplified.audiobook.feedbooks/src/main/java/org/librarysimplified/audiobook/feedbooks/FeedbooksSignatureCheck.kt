package org.librarysimplified.audiobook.feedbooks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.librarysimplified.audiobook.json_canon.JSONCanonicalization
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest

class FeedbooksSignatureCheck(
  private val manifest: PlayerManifest,
  private val onStatusChanged: (SingleLicenseCheckStatus) -> Unit
) : SingleLicenseCheckType {

  override fun execute(): SingleLicenseCheckResult {
    this.event("Started check")

    val signature =
      this.manifest.extensions.find { extension ->
        extension is FeedbooksSignature
      } as FeedbooksSignature?
        ?: return SingleLicenseCheckResult.NotApplicable("No signature information supplied.")

    this.event("Deserializing manifest bytes")
    val objectMapper = ObjectMapper()
    val objectNode = objectMapper.readTree(this.manifest.originalBytes) as ObjectNode
    objectNode.remove("http://www.feedbooks.com/audiobooks/signature")

    this.event("Canonicalizing manifest")
    val canonBytes = JSONCanonicalization.canonicalize(objectNode)

    return SingleLicenseCheckResult.NotApplicable("Not implemented!")
  }

  private fun event(message: String) {
    this.onStatusChanged.invoke(
      SingleLicenseCheckStatus(
        source = "FeedbooksSignatureCheck",
        message = message
      )
    )
  }
}
