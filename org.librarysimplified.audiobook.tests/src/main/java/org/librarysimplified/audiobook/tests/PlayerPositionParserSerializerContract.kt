package org.librarysimplified.audiobook.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerPositionParserType
import org.librarysimplified.audiobook.api.PlayerPositionSerializerType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerResult.Success
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
      PlayerPosition("A Title", 23, 137, 183991238L)
    )
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
      PlayerPosition("A Title", 23, 137, 183991238L)
    )

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
      PlayerPosition("A Title", 23, 137, 183991238L)
    )

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
      PlayerPosition("A Title", 23, 137, 183991238L)
    )

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
      PlayerPosition("A Title", 23, 137, 183991238L)
    )

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

  @Test
  fun testNullTitle() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition(null, 23, 137, 183991238L)
    )

    val result =
      parser.parseFromObjectNode(node)

    Assert.assertTrue(result is Success<PlayerPosition, Exception>)

    val resultNode = (result as Success<PlayerPosition, Exception>).result

    Assert.assertEquals(null, resultNode.title)
    Assert.assertEquals(23, resultNode.part)
    Assert.assertEquals(137, resultNode.chapter)
    Assert.assertEquals(183991238L, resultNode.offsetMilliseconds)
  }

  @Test
  fun testNullTitleExplicit() {
    val parser = createParser()
    val serial = createSerializer()

    val node = serial.serializeToObjectNode(
      PlayerPosition("Something", 23, 137, 183991238L)
    )

    val objectNode = node["position"] as ObjectNode
    objectNode.put("title", null as String?)

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
