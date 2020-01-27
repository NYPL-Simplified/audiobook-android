package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.vanilla.FRParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParserProviderType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParserType
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

/**
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class WebPubParserProvider : ManifestParserProviderType {

  private val logger =
    LoggerFactory.getLogger(WebPubParserProvider::class.java)

  private val fieldRushParsers =
    FRParsers()

  private val contextTypes =
    setOf(
      "http://readium.org/webpub/default.jsonld",
      "http://readium.org/webpub-manifest/context.jsonld"
    )

  private fun isRecognizedContextType(type: String): Boolean =
    this.contextTypes.contains(type)

  override fun canParse(
    uri: URI,
    streams: () -> InputStream
  ): Boolean {
    return streams.invoke().use { stream ->
      val contextParser =
        this.fieldRushParsers.createParser(
          uri = uri,
          stream = stream,
          rootParser = WebPubContextFinderParser()
        )

      when (val parseResult = contextParser.parse()) {
        is FRParseResult.FRParseSucceeded -> {
          val receivedTypes = parseResult.result
          for (type in receivedTypes) {
            if (isRecognizedContextType(type)) {
              return true
            }
          }

          this.logger.error(
            "none of the received types {} are present in {}",
            receivedTypes,
            this.contextTypes
          )
          false
        }

        is FRParseResult.FRParseFailed -> {
          for (error in parseResult.errors) {
            this.logger.error(
              "parse: {}: {}:{}:{}: {}: ",
              error.producer,
              error.position.source,
              error.position.line,
              error.position.column,
              error.message,
              error.exception
            )
          }
          false
        }
      }
    }
  }

  override fun createParser(
    uri: URI,
    streams: () -> InputStream,
    extensions: List<ManifestParserExtensionType>,
    warningsAsErrors: Boolean
  ): ParserType<PlayerManifest> {
    return WebPubParser(
      extensions = extensions,
      parsers = this.fieldRushParsers,
      stream = streams.invoke(),
      uri = uri
    )
  }
}
