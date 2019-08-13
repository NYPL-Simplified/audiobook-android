package org.librarysimplified.audiobook.api

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * A serializer of player positions.
 */

interface PlayerPositionSerializerType {

  /**
   * Serialize a player position to a JSON object node.
   *
   * @param position A player position
   * @return A serialized position
   */

  fun serializeToObjectNode(position: PlayerPosition): ObjectNode

}
