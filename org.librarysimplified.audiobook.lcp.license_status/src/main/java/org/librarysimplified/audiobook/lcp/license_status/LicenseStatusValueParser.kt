package org.librarysimplified.audiobook.lcp.license_status

import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.vanilla.FRValueParserScalar

/**
 * A value parser for status values.
 *
 * @see "https://readium.org/lcp-specs/releases/lsd/latest.html#23-status-of-a-license"
 */

class LicenseStatusValueParser(
  onReceive: (FRParserContextType, LicenseStatusDocument.Status) -> Unit
) : FRValueParserScalar<LicenseStatusDocument.Status>(onReceive) {

  override fun ofText(
    context: FRParserContextType,
    text: String
  ): FRParseResult<LicenseStatusDocument.Status> {
    val value = LicenseStatusDocument.Status.ofString(text)
      ?: return context.failureOf("Expected a 'status' value, received $text")
    return FRParseResult.succeed(value)
  }
}
