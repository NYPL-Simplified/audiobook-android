package org.librarysimplified.audiobook.rbdigital

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A link document parser.
 */

class RBDigitalLinkDocumentParser {

  private val mapper = ObjectMapper()

  /**
   * The result of parsing.
   */

  sealed class ParseResult {

    /**
     * Parsing succeeded.
     */

    data class ParseSuccess(val document: RBDigitalLinkDocument) : ParseResult()

    /**
     * Parsing failed.
     */

    data class ParseFailed(val exception: Exception) : ParseResult()
  }

  /**
   * Parse a document from the given object node.
   */

  fun parseFromObjectNode(node: ObjectNode): ParseResult {
    try {
      val type =
        if (node.has("type")) {
          node.get("type").textValue()
        } else {
          throw IOException("Missing key: type")
        }

      val url: URI =
        if (node.has("url")) {
          URI(node.get("url").textValue())
        } else {
          throw IOException("Missing key: url")
        }

      return ParseResult.ParseSuccess(RBDigitalLinkDocument(type = type, uri = url))
    } catch (e: Exception) {
      return ParseResult.ParseFailed(e)
    }
  }

  /**
   * Parse a document from the given file.
   */

  fun parseFromFile(file: File): ParseResult {
    try {
      return FileInputStream(file).use { stream -> this.parseFromStream(stream) }
    } catch (e: Exception) {
      return ParseResult.ParseFailed(e)
    }
  }

  /**
   * Parse a document from the given stream.
   */

  fun parseFromStream(stream: InputStream): ParseResult {
    try {
      return this.parseFromNode(this.mapper.readTree(stream))
    } catch (e: Exception) {
      return ParseResult.ParseFailed(e)
    }
  }

  /**
   * Parse a document from the given node.
   */

  fun parseFromNode(node: JsonNode): ParseResult {
    if (node is ObjectNode) {
      return this.parseFromObjectNode(node)
    } else {
      return ParseResult.ParseFailed(IOException(
        "Expected an object node, received " + node.nodeType))
    }
  }
}