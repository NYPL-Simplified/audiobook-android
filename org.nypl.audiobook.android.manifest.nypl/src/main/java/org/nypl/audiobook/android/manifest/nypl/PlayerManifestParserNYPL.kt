package org.nypl.audiobook.android.manifest.nypl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.nypl.audiobook.android.api.PlayerJSONParserUtilities
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifestEncrypted
import org.nypl.audiobook.android.api.PlayerManifestLink
import org.nypl.audiobook.android.api.PlayerManifestMetadata
import org.nypl.audiobook.android.api.PlayerManifestParserType
import org.nypl.audiobook.android.api.PlayerManifestScalar
import org.nypl.audiobook.android.api.PlayerManifestSpineItem
import org.nypl.audiobook.android.api.PlayerResult
import org.w3c.dom.Text

/**
 * A manifest parser that can parse manifests in the NYPL format.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class PlayerManifestParserNYPL : PlayerManifestParserType {

  override fun canParse(node: ObjectNode): Boolean {
    if (node.has("@context")) {
      val context = node["@context"]
      if (context is ArrayNode) {
        if (context.size() > 0) {
          for (element in context.elements()) {
            if (element is TextNode) {
              if (element.textValue() == "http://readium.org/webpub/default.jsonld") {
                return true
              }
            }
          }
        }
      } else if (context is TextNode) {
        return context.textValue() == "http://readium.org/webpub/default.jsonld"
      }
    }
    return false
  }

  override fun parseFromObjectNode(node: ObjectNode): PlayerResult<PlayerManifest, Exception> {
    try {
      val links = this.parseLinks(node)
      val spine = this.parseSpine(node)
      val metadata = this.parseMetadata(node)
      return PlayerResult.Success(PlayerManifest(spine = spine, links = links, metadata = metadata))
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
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
