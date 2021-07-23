package org.librarysimplified.audiobook.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY
import com.fasterxml.jackson.databind.node.JsonNodeType.BINARY
import com.fasterxml.jackson.databind.node.JsonNodeType.BOOLEAN
import com.fasterxml.jackson.databind.node.JsonNodeType.MISSING
import com.fasterxml.jackson.databind.node.JsonNodeType.NULL
import com.fasterxml.jackson.databind.node.JsonNodeType.NUMBER
import com.fasterxml.jackson.databind.node.JsonNodeType.OBJECT
import com.fasterxml.jackson.databind.node.JsonNodeType.POJO
import com.fasterxml.jackson.databind.node.JsonNodeType.STRING
import com.fasterxml.jackson.databind.node.ObjectNode
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarBoolean
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarNumber.PlayerManifestScalarInteger
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarNumber.PlayerManifestScalarReal
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarString
import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException

/**
 *
 * Utility functions for deserializing elements from JSON.
 *
 *
 * The functions take a strict approach: Types are checked upon key retrieval
 * and exceptions are raised if the type is not exactly as expected.
 */

@Deprecated("Use Jackson Databind or Fieldrush")
class PlayerJSONParserUtilities private constructor() {

  companion object {

    /**
     * Check that `n` is an object.
     *
     * @param key An optional advisory key to be used in error messages
     * @param n A node
     *
     * @return `n` as an [ObjectNode]
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun checkObject(
      key: String?,
      n: JsonNode
    ): ObjectNode {

      when (n.nodeType) {
        null,
        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        NUMBER,
        POJO,
        STRING -> {
          val sb = StringBuilder(128)
          if (key != null) {
            sb.append("Expected: A key '")
            sb.append(key)
            sb.append("' with a value of type Object\n")
            sb.append("Received: A value of type ")
            sb.append(n.nodeType)
            sb.append("\n")
          } else {
            sb.append("Expected: A value of type Object\n")
            sb.append("Received: A value of type ")
            sb.append(n.nodeType)
            sb.append("\n")
          }

          throw PlayerJSONParseException(sb.toString())
        }
        OBJECT -> {
          return n as ObjectNode
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param s A node
     *
     * @return An array from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getArray(
      s: ObjectNode,
      key: String
    ): ArrayNode {

      val n = getNode(s, key)
      when (n.nodeType) {
        ARRAY -> {
          return n as ArrayNode
        }
        null,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        NUMBER,
        POJO,
        STRING,
        OBJECT -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type Array\n")
          sb.append("Received: A value of type ")
          sb.append(n.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param o A node
     *
     * @return A boolean value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getBoolean(
      o: ObjectNode,
      key: String
    ): Boolean {

      val v = getNode(o, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        MISSING,
        NULL,
        OBJECT,
        POJO,
        STRING,
        NUMBER -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type Boolean\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
        BOOLEAN -> {
          return v.asBoolean()
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return An integer value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getInteger(
      n: ObjectNode,
      key: String
    ): Int {

      val v = getNode(n, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        OBJECT,
        POJO,
        STRING -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type Integer\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
        NUMBER -> {
          return v.asInt()
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param s A node
     *
     * @return An arbitrary json node from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getNode(
      s: ObjectNode,
      key: String
    ): JsonNode {

      if (s.has(key)) {
        return s.get(key)
      }

      val sb = StringBuilder(128)
      sb.append("Expected: A key '")
      sb.append(key)
      sb.append("'\n")
      sb.append("Received: nothing\n")
      throw PlayerJSONParseException(sb.toString())
    }

    /**
     * @param key A key assumed to be holding a value
     * @param s A node
     *
     * @return An object value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getObject(
      s: ObjectNode,
      key: String
    ): ObjectNode {

      val n = getNode(s, key)
      return checkObject(key, n)
    }

    /**
     * @param key A key assumed to be holding a value
     * @param s A node
     *
     * @return An object value from key `key`, if the key exists
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getObjectOptional(
      s: ObjectNode,
      key: String
    ): ObjectNode? {

      return if (s.has(key)) {
        getObject(s, key)
      } else null
    }

    /**
     * @param key A key assumed to be holding a value
     * @param s A node
     *
     * @return A string value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getString(
      s: ObjectNode,
      key: String
    ): String {

      val v = getNode(s, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        NUMBER,
        OBJECT,
        POJO -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type String\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
        STRING -> {
          return v.asText()
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return An integer value from key `key`, if the key exists
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getIntegerOptional(
      n: ObjectNode,
      key: String
    ): Int? {

      return if (n.has(key)) {
        getInteger(n, key)
      } else null
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A string value from key `key`, if the key exists
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getStringOptional(
      n: ObjectNode,
      key: String
    ): String? {

      val v = n[key] ?: return null
      return when (v.nodeType) {
        null,
        NULL ->
          null

        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NUMBER,
        OBJECT,
        POJO -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type String\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }

        STRING ->
          v.asText()
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A URI value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getURI(
      n: ObjectNode,
      key: String
    ): URI {

      try {
        return URI(getString(n, key))
      } catch (e: URISyntaxException) {
        throw PlayerJSONParseException(e)
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     * @param v A default value
     *
     * @return A boolean from key `key`, or `v` if the key does not
     * exist
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getBooleanDefault(
      n: ObjectNode,
      key: String,
      v: Boolean
    ): Boolean {

      return if (n.has(key)) {
        getBoolean(n, key)
      } else v
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A big integer value from key `key`, if the key exists
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getBigIntegerOptional(
      n: ObjectNode,
      key: String
    ): BigInteger? {

      return if (n.has(key)) {
        getBigInteger(n, key)
      } else null
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A big integer value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getBigInteger(
      n: ObjectNode,
      key: String
    ): BigInteger {

      val v = getNode(n, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        OBJECT,
        POJO,
        STRING -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type Integer\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
        NUMBER -> {
          try {
            return BigInteger(v.asText())
          } catch (e: NumberFormatException) {
            throw PlayerJSONParseException(e)
          }
        }
      }
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A big integer value from key `key`, if the key exists
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getDoubleOptional(
      n: ObjectNode,
      key: String
    ): Double? {

      return if (n.has(key)) {
        getDouble(n, key)
      } else null
    }

    /**
     * @param key A key assumed to be holding a value
     * @param n A node
     *
     * @return A big integer value from key `key`
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getDouble(
      n: ObjectNode,
      key: String
    ): Double {

      val v = getNode(n, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        BOOLEAN,
        MISSING,
        NULL,
        OBJECT,
        POJO,
        STRING -> {
          val sb = StringBuilder(128)
          sb.append("Expected: A key '")
          sb.append(key)
          sb.append("' with a value of type Double\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }
        NUMBER -> {
          return v.asDouble()
        }
      }
    }

    /**
     * @param n A node
     *
     * @return A scalar value from the given node
     *
     * @throws PlayerJSONParseException On type errors
     */

    @Throws(PlayerJSONParseException::class)
    fun getScalar(
      n: ObjectNode,
      key: String
    ): PlayerManifestScalar? {

      val v = getNode(n, key)
      when (v.nodeType) {
        null,
        ARRAY,
        BINARY,
        MISSING,
        OBJECT,
        POJO -> {
          val sb = StringBuilder(128)
          sb.append("Expected: An object of a scalar type.\n")
          sb.append("Received: A value of type ")
          sb.append(v.nodeType)
          sb.append("\n")
          sb.append("Key: ")
          sb.append(key)
          sb.append("\n")
          throw PlayerJSONParseException(sb.toString())
        }

        NULL -> return null
        BOOLEAN -> return PlayerManifestScalarBoolean(v.asBoolean())
        NUMBER ->
          if (v.isIntegralNumber) {
            return PlayerManifestScalarInteger(v.asInt())
          } else {
            return PlayerManifestScalarReal(v.asDouble())
          }
        STRING -> return PlayerManifestScalarString(v.asText())
      }
    }
  }
}
