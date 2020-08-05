package org.librarysimplified.audiobook.json_canon

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * JSON canonicalization functions.
 *
 * @see "https://tools.ietf.org/id/draft-rundgren-json-canonicalization-scheme-00.html"
 */

object JSONCanonicalization {

  /**
   * Serialize the given JSON node as canonical JSON.
   */

  fun canonicalize(
    objectNode: ObjectNode,
    outputStream: OutputStream
  ) {
    val mapper = ObjectMapper()
    mapper.configure(SerializationFeature.INDENT_OUTPUT, false)
    mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper.writeValue(outputStream, objectNode)
  }

  /**
   * Serialize the given JSON node as canonical JSON.
   */

  fun canonicalize(objectNode: ObjectNode): ByteArray {
    return ByteArrayOutputStream().use { stream ->
      canonicalize(objectNode, stream)
      stream.toByteArray()
    }
  }
}
