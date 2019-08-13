package org.librarysimplified.audiobook.api

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * The type of parsers for player manifests.
 */

interface PlayerManifestParserType {

  /**
   * @return true If this parser thinks it can handle the data in the given JSON node
   */

  fun canParse(node: ObjectNode): Boolean

  /**
   * Parse a manifest from the given JSON object node.
   *
   * @param node An object node
   * @return A parsed manifest, or a reason why it couldn't be parsed
   */

  fun parseFromObjectNode(node: ObjectNode): PlayerResult<PlayerManifest, Exception>

}