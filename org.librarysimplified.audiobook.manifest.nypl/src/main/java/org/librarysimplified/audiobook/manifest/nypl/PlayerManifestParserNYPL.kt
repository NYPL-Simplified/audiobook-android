package org.librarysimplified.audiobook.manifest.nypl

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.librarysimplified.audiobook.api.PlayerJSONParserUtilities
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.api.PlayerManifestMetadata
import org.librarysimplified.audiobook.api.PlayerManifestParserType
import org.librarysimplified.audiobook.api.PlayerManifestScalar
import org.librarysimplified.audiobook.api.PlayerManifestSpineItem
import org.librarysimplified.audiobook.api.PlayerResult
import org.slf4j.LoggerFactory

/**
 * A manifest parser that can parse manifests in the NYPL format.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class PlayerManifestParserNYPL : PlayerManifestParserType {

  private val log = LoggerFactory.getLogger(PlayerManifestParserNYPL::class.java)

  private val contextTypes = setOf(
    "http://readium.org/webpub/default.jsonld",
    "http://readium.org/webpub-manifest/context.jsonld")

  private fun isRecognizedContextType(type: String): Boolean {
    return this.contextTypes.contains(type)
  }

  override fun canParse(node: ObjectNode): Boolean {
    if (node.has("@context")) {
      val context = node["@context"]
      if (context is ArrayNode) {
        if (context.size() > 0) {
          for (element in context.elements()) {
            if (element is TextNode) {
              if (this.isRecognizedContextType(element.textValue())) {
                return true
              }
            }
          }
        }
      } else if (context is TextNode) {
        return this.isRecognizedContextType(context.textValue())
      }
    }

    this.log.debug("could not find an acceptable @context value")
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
      if (node[key].isValueNode) {
        val value = PlayerJSONParserUtilities.getScalar(node, key)
        if (value != null) {
          values.put(key, value)
        }
      }
    }
    return values.toMap()
  }
}
