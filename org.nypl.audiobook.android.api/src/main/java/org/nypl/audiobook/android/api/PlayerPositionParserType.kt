package org.nypl.audiobook.android.api

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * A parser of player positions.
 */

interface PlayerPositionParserType {

  /**
   * Parse a player position from the given JSON object node.
   *
   * @param node An object node
   * @return A parsed player position, or a reason why it couldn't be parsed
   */

  fun parseFromObjectNode(node: ObjectNode): PlayerResult<PlayerPosition, Exception>

}
