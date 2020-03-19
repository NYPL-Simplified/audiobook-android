package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParserProviderType
import org.librarysimplified.audiobook.parser.api.ParserType
import java.net.URI

/**
 * The type of manifest parser providers.
 */

interface ManifestParserProviderType :
  ParserProviderType<ByteArray, ManifestParserExtensionType, PlayerManifest> {

  /**
   * The base format supported by this parser provider.
   */

  val format: String

  /**
   * Return `true` if this parser is capable of parsing the given input.
   */

  fun canParse(
    uri: URI,
    input: ByteArray
  ): Boolean

  /**
   * Create a new parser for the given input.
   */

  override fun createParser(
    uri: URI,
    input: ByteArray,
    extensions: List<ManifestParserExtensionType>,
    warningsAsErrors: Boolean
  ): ParserType<PlayerManifest>
}
