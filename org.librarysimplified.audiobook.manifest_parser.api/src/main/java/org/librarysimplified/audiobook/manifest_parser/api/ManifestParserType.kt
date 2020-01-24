package org.librarysimplified.audiobook.manifest_parser.api

import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.parser.api.ParserType

/**
 * The type of manifest parsers.
 */

interface ManifestParserType : ParserType<PlayerManifest>
