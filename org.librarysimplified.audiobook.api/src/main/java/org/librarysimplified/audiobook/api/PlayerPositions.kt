package org.librarysimplified.audiobook.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Functions to serialize and parse player positions.
 */

object PlayerPositions : PlayerPositionParserType, PlayerPositionSerializerType {

  override fun parseFromObjectNode(node: ObjectNode): PlayerResult<PlayerPosition, Exception> {
    try {
      val version = PlayerJSONParserUtilities.getInteger(node, "@version")
      when (version) {
        1 -> {
          return parseFromObjectNodeV1(node)
        }
      }

      throw PlayerJSONParseException("Unsupported format version: $version")
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
    }
  }

  @Throws(PlayerJSONParseException::class)
  private fun parseFromObjectNodeV1(node: ObjectNode): PlayerResult<PlayerPosition, Exception> {
    val positionNode = PlayerJSONParserUtilities.getObject(node, "position")

    val chapter =
      PlayerJSONParserUtilities.getInteger(positionNode, "chapter")
    val part =
      PlayerJSONParserUtilities.getInteger(positionNode, "part")
    val offsetMilliseconds =
      PlayerJSONParserUtilities.getBigInteger(positionNode, "offsetMilliseconds").toLong()
    val title =
      PlayerJSONParserUtilities.getStringOptional(positionNode, "title")

    return PlayerResult.Success(PlayerPosition(
      title = title,
      part = part,
      chapter = chapter,
      offsetMilliseconds = offsetMilliseconds))
  }

  override fun serializeToObjectNode(position: PlayerPosition): ObjectNode {
    val objects = ObjectMapper()
    val node = objects.createObjectNode()
    node.put("@version", 1)

    val positionNode = objects.createObjectNode()
    positionNode.put("chapter", position.chapter)
    positionNode.put("part", position.part)
    positionNode.put("offsetMilliseconds", position.offsetMilliseconds)
    positionNode.put("title", position.title)

    node.set("position", positionNode)
    return node
  }

}