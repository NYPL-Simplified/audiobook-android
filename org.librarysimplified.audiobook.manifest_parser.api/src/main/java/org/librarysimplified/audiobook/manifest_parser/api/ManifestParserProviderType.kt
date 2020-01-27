package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParserProviderType
import org.librarysimplified.audiobook.parser.api.ParserType
import java.io.InputStream
import java.net.URI

/**
 * The type of manifest parser providers.
 */

interface ManifestParserProviderType
  : ParserProviderType<InputStream, ManifestParserExtensionType, PlayerManifest> {

  fun canParse(
    uri: URI,
    streams: () -> InputStream
  ): Boolean

  override fun createParser(
    uri: URI,
    streams: () -> InputStream,
    extensions: List<ManifestParserExtensionType>,
    warningsAsErrors: Boolean
  ): ParserType<PlayerManifest>

}
