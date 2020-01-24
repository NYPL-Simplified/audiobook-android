package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseError
import one.irradia.fieldrush.api.FRParseResult.FRParseFailed
import one.irradia.fieldrush.api.FRParseResult.FRParseSucceeded
import one.irradia.fieldrush.api.FRParserProviderType
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParserType
import org.librarysimplified.audiobook.parser.api.ParseError
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.io.InputStream
import java.net.URI

class WebPubParser(
  private val parsers: FRParserProviderType,
  private val stream: InputStream,
  private val uri: URI
) : ManifestParserType {

  override fun parse(): ParseResult<PlayerManifest> {
    val result =
      this.parsers.createParser(this.uri, this.stream, WebPubManifestParser())
        .parse()

    return when (result) {
      is FRParseSucceeded -> {
        ParseResult.Success(
          warnings = listOf(),
          result = result.result
        )
      }
      is FRParseFailed -> {
        ParseResult.Failure(
          warnings = listOf(),
          errors = result.errors.map { error -> toParseError(error) },
          result = null
        )
      }
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
