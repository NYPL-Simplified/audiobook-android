package org.librarysimplified.audiobook.tests

import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.feedbooks.FeedbooksRightsCheck
import org.librarysimplified.audiobook.feedbooks.FeedbooksSignatureCheck
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.Logger
import java.net.URI
import java.util.ServiceLoader

abstract class FeedbooksSignatureCheckContract {

  private lateinit var eventLog: MutableList<SingleLicenseCheckStatus>

  abstract fun log(): Logger

  @Before
  fun testSetup() {
    this.eventLog = mutableListOf()
  }

  @Test
  fun testOK() {
    val manifest = this.manifest("feedbooks_0.json")

    val result =
      FeedbooksSignatureCheck(
        parameters = SingleLicenseCheckParameters(
          manifest = manifest,
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { }
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Succeeded)
  }

  private fun manifest(
    name: String
  ): PlayerManifest {
    val result =
      ManifestParsers.parse(
        uri = URI.create(name),
        streams = this.resource(name),
        extensions = ServiceLoader.load(ManifestParserExtensionType::class.java).toList()
      )
    this.log().debug("result: {}", result)
    Assert.assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    return success.result
  }

  private fun resource(
    name: String
  ): ByteArray {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return FeedbooksSignatureCheckContract::class.java.getResourceAsStream(path)?.readBytes()
      ?: throw AssertionError("Missing resource file: " + path)
  }
}
