package org.librarysimplified.audiobook.lcp.license_status

import one.irradia.fieldrush.vanilla.FRParsers
import org.librarysimplified.audiobook.parser.api.ParserType
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * The default implementation of the [LicenseStatusParserProviderType] interface.
 */

object LicenseStatusParsers : LicenseStatusParserProviderType {

  private val fieldRushParsers =
    FRParsers()

  override fun createParser(
    uri: URI,
    input: ByteArray,
    extensions: List<Any>,
    warningsAsErrors: Boolean
  ): ParserType<LicenseStatusDocument> {
    return LicenseStatusParser(
      parsers = this.fieldRushParsers,
      stream = ByteArrayInputStream(input),
      uri = uri
    )
  }
}
