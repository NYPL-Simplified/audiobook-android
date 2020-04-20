package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.net.URI

/**
 * Functions to parse manifests.
 */

interface ManifestParsersType {

  /**
   * Parse a manifest from the given input stream. This will try each of the available
   * parser providers in turn until one claims that it can parse the resulting manifest. Parser
   * extensions will be loaded from [ServiceLoader].
   */

  fun parse(
    uri: URI,
    streams: ByteArray
  ): ParseResult<PlayerManifest>

  /**
   * Parse a manifest from the given input stream. This will try each of the available
   * parser providers in turn until one claims that it can parse the resulting manifest.
   */

  fun parse(
    uri: URI,
    streams: ByteArray,
    extensions: List<ManifestParserExtensionType>
  ): ParseResult<PlayerManifest>
}
