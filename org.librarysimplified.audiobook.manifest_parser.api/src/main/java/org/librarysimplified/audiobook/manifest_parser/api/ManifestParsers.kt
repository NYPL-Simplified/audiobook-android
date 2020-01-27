package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseError
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ServiceLoader

/**
 * Functions to parse manifests.
 */

object ManifestParsers {

  private val logger =
    LoggerFactory.getLogger(ManifestParsers::class.java)

  /**
   * Parse a manifest from the given input stream. This will try each of the available
   * parser providers in turn until one claims that it can parse the resulting manifest.
   */

  fun parse(
    uri: URI,
    streams: () -> InputStream,
    extensions: List<ManifestParserExtensionType>
    ): ParseResult<PlayerManifest> {
    try {
      val providers: List<ManifestParserProviderType> =
        ServiceLoader.load(ManifestParserProviderType::class.java)
          .toList()

      for (provider in providers) {
        this.logger.debug(
          "checking if provider {} can parse {}",
          provider.javaClass.canonicalName,
          uri)

        if (provider.canParse(uri, streams)) {
          this.logger.debug("parsing with provider {}", provider.javaClass.canonicalName)
          return provider.createParser(
            uri = uri,
            streams = streams,
            extensions = extensions.filter {
              extension -> extension.format == provider.format
            },
            warningsAsErrors = false
          ).parse()
        }
      }

      return ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(
          errorOfException(
            uri,
            IOException("Could not find a usable parser provider for the given manifest")
          )
        ),
        result = null
      )
    } catch (e: Exception) {
      return ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(errorOfException(uri, e)),
        result = null
      )
    }
  }

  private fun errorOfException(
    uri: URI,
    exception: Exception
  ): ParseError {
    return ParseError(
      source = uri,
      message = exception.message ?: exception.javaClass.name,
      line = 0,
      column = 0,
      exception = exception
    )
  }
}