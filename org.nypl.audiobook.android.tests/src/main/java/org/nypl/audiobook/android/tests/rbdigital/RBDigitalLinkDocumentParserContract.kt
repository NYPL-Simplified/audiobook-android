package org.nypl.audiobook.android.tests.rbdigital

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert
import org.junit.Test
import org.nypl.audiobook.rbdigital.RBDigitalLinkDocumentParser
import org.nypl.audiobook.rbdigital.RBDigitalLinkDocumentParser.ParseResult.ParseFailed
import org.nypl.audiobook.rbdigital.RBDigitalLinkDocumentParser.ParseResult.ParseSuccess
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

abstract class RBDigitalLinkDocumentParserContract {

  private val mapper = ObjectMapper()

  @Test
  fun parseFromObjectNode() {
    val parser = RBDigitalLinkDocumentParser()

    val objectNode = this.mapper.createObjectNode()
    objectNode.put("type", "application/octet-stream")
    objectNode.put("url", "http://www.example.com")

    val result = parser.parseFromObjectNode(objectNode)
    Assert.assertTrue(result is ParseSuccess)
    val document = result as ParseSuccess
    Assert.assertEquals("application/octet-stream", document.document.type)
    Assert.assertEquals("http://www.example.com", document.document.uri.toString())
  }

  @Test
  fun parseFromObjectNodeMissingType() {
    val parser = RBDigitalLinkDocumentParser()

    val objectNode = this.mapper.createObjectNode()
    objectNode.put("url", "http://www.example.com")

    val result = parser.parseFromObjectNode(objectNode)
    Assert.assertTrue(result is ParseFailed)
  }

  @Test
  fun parseFromObjectNodeMissingURL() {
    val parser = RBDigitalLinkDocumentParser()

    val objectNode = this.mapper.createObjectNode()
    objectNode.put("type", "application/octet-stream")

    val result = parser.parseFromObjectNode(objectNode)
    Assert.assertTrue(result is ParseFailed)
  }

  @Test
  fun parseFromNodeNotObject() {
    val parser = RBDigitalLinkDocumentParser()

    val node = this.mapper.createArrayNode()

    val result = parser.parseFromNode(node)
    Assert.assertTrue(result is ParseFailed)
  }

  @Test
  fun parseFromStream() {
    val parser = RBDigitalLinkDocumentParser()

    val objectNode = this.mapper.createObjectNode()
    objectNode.put("type", "application/octet-stream")
    objectNode.put("url", "http://www.example.com")

    ByteArrayInputStream(this.mapper.writeValueAsBytes(objectNode)).use { stream ->
      val result = parser.parseFromStream(stream)
      Assert.assertTrue(result is ParseSuccess)
      val document = result as ParseSuccess
      Assert.assertEquals("application/octet-stream", document.document.type)
      Assert.assertEquals("http://www.example.com", document.document.uri.toString())
    }
  }

  @Test
  fun parseFromFile() {
    val parser = RBDigitalLinkDocumentParser()

    val objectNode = this.mapper.createObjectNode()
    objectNode.put("type", "application/octet-stream")
    objectNode.put("url", "http://www.example.com")

    val file = File.createTempFile("rbdigital-test-", ".tmp")
    FileOutputStream(file).use { output -> this.mapper.writeValue(output, objectNode) }

    FileInputStream(file).use { stream ->
      val result = parser.parseFromStream(stream)
      Assert.assertTrue(result is ParseSuccess)
      val document = result as ParseSuccess
      Assert.assertEquals("application/octet-stream", document.document.type)
      Assert.assertEquals("http://www.example.com", document.document.uri.toString())
    }
  }
}
