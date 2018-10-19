package org.nypl.audiobook.android.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert
import org.junit.Test
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerPositionParserType
import org.nypl.audiobook.android.api.PlayerPositionSerializerType
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.api.PlayerResult.Success
import org.slf4j.Logger

abstract class PlayerPositionParserSerializerContract {

  abstract fun createParser(): PlayerPositionParserType

  abstract fun createSerializer(): PlayerPositionSerializerType

  abstract fun logger(): Logger

  @Test
  fun testSimpleRoundTrip() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("A Title", 23, 137, 183991238L))
    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is Success<PlayerPosition, Exception>)

    val resultNode = (result as Success<PlayerPosition, Exception>).result

    Assert.assertEquals("A Title", resultNode.title)
    Assert.assertEquals(23, resultNode.part)
    Assert.assertEquals(137, resultNode.chapter)
    Assert.assertEquals(183991238L, resultNode.offsetMilliseconds)
  }

  @Test
  fun testMissingVersion() {
    val parser = createParser()

    val objects = ObjectMapper()
    val node = objects.createObjectNode()

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is PlayerResult.Failure<PlayerPosition, Exception>)
  }

  @Test
  fun testUnsupportedVersion() {
    val parser = createParser()

    val objects = ObjectMapper()
    val node = objects.createObjectNode()
    node.put("@version", Integer.MAX_VALUE)

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is PlayerResult.Failure<PlayerPosition, Exception>)
  }

  @Test
  fun testMissingPart() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("A Title", 23, 137, 183991238L))

    (node["position"] as ObjectNode).remove("part")

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is PlayerResult.Failure<PlayerPosition, Exception>)
  }

  @Test
  fun testMissingChapter() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("A Title", 23, 137, 183991238L))

    (node["position"] as ObjectNode).remove("chapter")

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is PlayerResult.Failure<PlayerPosition, Exception>)
  }

  @Test
  fun testMissingOffset() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("A Title", 23, 137, 183991238L))

    (node["position"] as ObjectNode).remove("offsetMilliseconds")

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is PlayerResult.Failure<PlayerPosition, Exception>)
  }

  @Test
  fun testMissingTitle() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("A Title", 23, 137, 183991238L))

    (node["position"] as ObjectNode).remove("title")

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is Success<PlayerPosition, Exception>)

    val resultNode = (result as Success<PlayerPosition, Exception>).result

    Assert.assertEquals(null, resultNode.title)
    Assert.assertEquals(23, resultNode.part)
    Assert.assertEquals(137, resultNode.chapter)
    Assert.assertEquals(183991238L, resultNode.offsetMilliseconds)
  }
}
