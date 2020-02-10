package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusDocument
import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusParserProviderType
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.net.URI

abstract class LicenseStatusParserContract {

  abstract fun parsers(): LicenseStatusParserProviderType

  @Test
  fun testBasicDocument() {
    val parsers =
      this.parsers()
    val parser =
      parsers.createParser(URI.create("urn:test"), resource("lcp_license_status.json"))
    val result =
      parser.parse() as ParseResult.Success

    val document = result.result
    Assert.assertEquals(LicenseStatusDocument.Status.ACTIVE, document.status)
  }

  @Test
  fun testBasicDocumentUnspecifiedStatus() {
    val parsers =
      this.parsers()
    val parser =
      parsers.createParser(URI.create("urn:test"), resource("lcp_license_status_unspec.json"))
    val result =
      parser.parse() as ParseResult.Success

    val document = result.result
    Assert.assertEquals(LicenseStatusDocument.Status.ACTIVE, document.status)
  }

  @Test
  fun testBasicDocumentRevoked() {
    val parsers =
      this.parsers()
    val parser =
      parsers.createParser(URI.create("urn:test"), resource("lcp_license_status_revoked.json"))
    val result =
      parser.parse() as ParseResult.Success

    val document = result.result
    Assert.assertEquals(LicenseStatusDocument.Status.REVOKED, document.status)
  }

  private fun resource(
    name: String
  ): ByteArray {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return LicenseCheckContract::class.java.getResourceAsStream(path)?.readBytes()
      ?: throw AssertionError("Missing resource file: " + path)
  }
}
