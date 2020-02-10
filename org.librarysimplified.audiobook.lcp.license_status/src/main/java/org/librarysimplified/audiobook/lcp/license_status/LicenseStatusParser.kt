package org.librarysimplified.audiobook.lcp.license_status

import one.irradia.fieldrush.api.FRParseError
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserProviderType
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.parser.api.ParseError
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.io.InputStream
import java.net.URI

/**
 * The basic license status document parser.
 */

class LicenseStatusParser(
  private val parsers: FRParserProviderType,
  private val stream: InputStream,
  private val uri: URI
) : LicenseStatusParserType {

  override fun parse(): ParseResult<LicenseStatusDocument> {
    val result =
      this.parsers.createParser(
        uri = this.uri,
        stream = this.stream,
        rootParser = LicenseStatusDocumentParser(
          valueParsers = FRValueParsers
        )
      ).parse()

    return when (result) {
      is FRParseResult.FRParseSucceeded ->
        ParseResult.Success(
          warnings = listOf(),
          result = result.result
        )
      is FRParseResult.FRParseFailed ->
        ParseResult.Failure(
          warnings = listOf(),
          errors = result.errors.map { error -> this.toParseError(error) },
          result = null
        )
    }
  }

  private fun toParseError(error: FRParseError): ParseError {
    return ParseError(
      source = error.position.source,
      message = error.message,
      line = error.position.line,
      column = error.position.column,
      exception = error.exception
    )
  }

  override fun close() {
    this.stream.close()
  }
}
