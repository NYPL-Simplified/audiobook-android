package org.librarysimplified.audiobook.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.IOException
import java.io.InputStream
import java.util.ServiceLoader

/**
 * Functions to parse manifests.
 */

object PlayerManifests {

  private val providers: MutableList<PlayerManifestParserType> =
    ServiceLoader.load(PlayerManifestParserType::class.java).toMutableList()

  private fun findParser(node: ObjectNode): PlayerManifestParserType? {
    for (provider in providers) {
      if (provider.canParse(node)) {
        return provider
      }
    }
    return null
  }

  /**
   * Parse a manifest from the given input stream. This will try each of the available
   * parser providers in turn until one claims that it can parse the resulting JSON manifest.
   */

  fun parse(stream: InputStream): PlayerResult<PlayerManifest, Exception> {
    try {
      val mapper = ObjectMapper()
      val result = mapper.readTree(stream)
      if (result is ObjectNode) {
        val provider = findParser(result)
        if (provider == null) {
          return PlayerResult.Failure(IOException(
            "Could not find a usable parser provider for the given manifest"))
        }
        return provider.parseFromObjectNode(result)
      }
      return PlayerResult.Failure(IOException(
        "Expected a JSON object but received a " + result.nodeType))
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
    }
  }
}