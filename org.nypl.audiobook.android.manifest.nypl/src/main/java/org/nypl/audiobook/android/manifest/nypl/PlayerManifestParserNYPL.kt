package org.nypl.audiobook.android.manifest.nypl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.nypl.audiobook.android.api.PlayerJSONParserUtilities
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifestEncrypted
import org.nypl.audiobook.android.api.PlayerManifestMetadata
import org.nypl.audiobook.android.api.PlayerManifestParserType
import org.nypl.audiobook.android.api.PlayerManifestScalar
import org.nypl.audiobook.android.api.PlayerManifestSpineItem
import org.nypl.audiobook.android.api.PlayerResult

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
      val spine = this.parseSpine(node)
      val metadata = this.parseMetadata(node)
      return PlayerResult.Success(PlayerManifest(spine = spine, metadata = metadata))
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
    }
  }

  private fun parseMetadata(node: ObjectNode): PlayerManifestMetadata {
    val metadata = PlayerJSONParserUtilities.getObject(node, "metadata")

    val title = PlayerJSONParserUtilities.getString(metadata, "title")
    val identifier = PlayerJSONParserUtilities.getString(metadata, "identifier")
    val encrypted = this.parseEncrypted(metadata)

    return PlayerManifestMetadata(
      title = title,
      identifier = identifier,
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

  private fun parseSpine(node: ObjectNode): List<PlayerManifestSpineItem> {
    val spine_array = PlayerJSONParserUtilities.getArray(node, "readingOrder")
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

  private fun parseScalarMap(node: ObjectNode): Map<String, PlayerManifestScalar> {
    val values = HashMap<String, PlayerManifestScalar>()
    for (key in node.fieldNames()) {
      val value = PlayerJSONParserUtilities.getScalar(node, key)
      if (value != null) {
        values.put(key, value)
      }
    }
    return values.toMap()
  }
}
