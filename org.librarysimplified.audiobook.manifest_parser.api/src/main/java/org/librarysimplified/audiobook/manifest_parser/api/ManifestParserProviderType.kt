package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.parser.api.ParserProviderType
import org.librarysimplified.audiobook.parser.api.ParserType
import java.io.InputStream
import java.net.URI

/**
 * The type of manifest parser providers.
 */

interface ManifestParserProviderType : ParserProviderType<InputStream, PlayerManifest> {

  fun canParse(
    uri: URI,
    streams: () -> InputStream
  ): Boolean

  override fun createParser(
    uri: URI,
    streams: () -> InputStream,
    warningsAsErrors: Boolean
  ): ParserType<PlayerManifest>

}
