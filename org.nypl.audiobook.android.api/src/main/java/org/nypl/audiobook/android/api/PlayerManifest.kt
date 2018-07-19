package org.nypl.audiobook.android.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.nypl.audiobook.android.api.PlayerResult.Failure
import org.nypl.audiobook.android.api.PlayerResult.Success
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * A raw audio book manifest, parsed and typed.
 */

data class PlayerManifest(
  val spine: List<PlayerManifestSpineItem>,
  val links: List<PlayerManifestLink>,
  val metadata: PlayerManifestMetadata) {

  companion object {

    /**
     * Parse a manifest from the given stream.
     *
     * @param stream An input stream
     * @return A parsed manifest, or a reason why it couldn't be parsed
     */

    fun parse(stream: InputStream): PlayerResult<PlayerManifest, Exception> {
      try {
        val mapper = ObjectMapper()
        val result = mapper.readTree(stream)
        if (result is ObjectNode) {
          return this.parseFromObjectNode(result)
        }
        return Failure(IOException(
          "Expected a JSON object but received a " + result.nodeType))
      } catch (e: Exception) {
        return Failure(e)
      }
    }

    /**
     * Parse a manifest from the given object node.
     *
     * @param node An object node
     * @return A parsed manifest, or a reason why it couldn't be parsed
     */

    fun parseFromObjectNode(node: ObjectNode): PlayerResult<PlayerManifest, Exception> {
      try {
        val links = this.parseLinks(node)
        val spine = this.parseSpine(node)
        val metadata = this.parseMetadata(node)
        return Success(PlayerManifest(spine = spine, links = links, metadata = metadata))
      } catch (e: Exception) {
        return Failure(e)
      }
    }

    private fun parseMetadata(node: ObjectNode): PlayerManifestMetadata {
      val metadata = PlayerJSONParserUtilities.getObject(node, "metadata")

      val title = PlayerJSONParserUtilities.getString(metadata, "title")
      val language = PlayerJSONParserUtilities.getString(metadata, "language")
      val duration = PlayerJSONParserUtilities.getDouble(metadata, "duration")
      val identifier = PlayerJSONParserUtilities.getString(metadata, "identifier")
      val authors = this.parseAuthors(metadata)
      val encrypted = this.parseEncrypted(metadata)

      return PlayerManifestMetadata(
        title = title,
        language = language,
        duration = duration,
        identifier = identifier,
        authors = authors,
        encrypted = encrypted)
    }

    private fun parseEncrypted(node: ObjectNode): PlayerManifestEncrypted? {
      val encrypted = PlayerJSONParserUtilities.getObjectOptional(node, "encrypted")
      if (encrypted == null) {
        return null
      }

      val scheme = PlayerJSONParserUtilities.getString(encrypted, "scheme")
      val values = this.parseScalarMap(encrypted)
      return PlayerManifestEncrypted(scheme, values)
    }

    private fun parseAuthors(node: ObjectNode): List<String> {
      val author_array = PlayerJSONParserUtilities.getArray(node, "authors")
      val authors = ArrayList<String>()

      for (index in 0..author_array.size() - 1) {
        if (author_array[index] is TextNode) {
          authors.add(author_array[index].asText())
        }
      }
      return authors.toList()
    }

    private fun parseSpine(node: ObjectNode): List<PlayerManifestSpineItem> {
      val spine_array = PlayerJSONParserUtilities.getArray(node, "spine")
      val spines = ArrayList<PlayerManifestSpineItem>()

      for (index in 0..spine_array.size() - 1) {
        spines.add(this.parsePlayerManifestSpineItem(
          PlayerJSONParserUtilities.checkObject(null, spine_array[index])))
      }
      return spines.toList()
    }

    private fun parsePlayerManifestSpineItem(node: ObjectNode): PlayerManifestSpineItem {
      return PlayerManifestSpineItem(this.parseScalarMap(node))
    }

    private fun parseScalarMap(node: ObjectNode): Map<String, PlayerManifestScalar?> {
      val values = HashMap<String, PlayerManifestScalar?>()
      for (key in node.fieldNames()) {
        values.put(key, PlayerJSONParserUtilities.getScalar(node, key))
      }
      return values.toMap()
    }

    private fun parseLinks(node: ObjectNode): List<PlayerManifestLink> {
      val link_array = PlayerJSONParserUtilities.getArray(node, "links")
      val links = ArrayList<PlayerManifestLink>()

      for (index in 0..link_array.size() - 1) {
        links.add(this.parsePlayerManifestLink(
          PlayerJSONParserUtilities.checkObject(null, link_array[index])))
      }
      return links.toList()
    }

    private fun parsePlayerManifestLink(node: ObjectNode): PlayerManifestLink {
      return PlayerManifestLink(
        href = PlayerJSONParserUtilities.getURI(node, "href"),
        relation = PlayerJSONParserUtilities.getString(node, "rel"))
    }
  }
}

/**
 * The metadata section in a manifest.
 */

data class PlayerManifestMetadata(
  val title: String,
  val language: String,
  val duration: Double,
  val identifier: String,
  val authors: List<String>,
  val encrypted: PlayerManifestEncrypted?)

/**
 * A section in a manifest dealing with encryption details.
 */

data class PlayerManifestEncrypted(
  val scheme: String,
  val values: Map<String, PlayerManifestScalar?>)

/**
 * A link appearing in a manifest.
 */

data class PlayerManifestLink(
  val href: URI,
  val relation: String)

/**
 * A spine item.
 */

data class PlayerManifestSpineItem(
  val values: Map<String, PlayerManifestScalar?>)

/**
 * A scalar value appearing in a manifest.
 */

sealed class PlayerManifestScalar {

  /**
   * A string-typed scalar manifest value.
   */

  data class PlayerManifestScalarString(val text: String) : PlayerManifestScalar() {
    override fun toString(): String {
      return this.text
    }
  }

  /**
   * A number-typed scalar manifest value.
   */

  sealed class PlayerManifestScalarNumber : PlayerManifestScalar() {

    /**
     * A real-typed scalar manifest value.
     */

    data class PlayerManifestScalarReal(val number: Double) : PlayerManifestScalarNumber() {
      override fun toString(): String {
        return this.number.toString()
      }
    }

    /**
     * An integer-typed scalar manifest value.
     */

    data class PlayerManifestScalarInteger(val number: Int) : PlayerManifestScalarNumber() {
      override fun toString(): String {
        return this.number.toString()
      }
    }
  }

  /**
   * A boolean-typed scalar manifest value.
   */

  data class PlayerManifestScalarBoolean(val value: Boolean) : PlayerManifestScalar() {
    override fun toString(): String {
      return this.value.toString()
    }
  }
}