package org.nypl.audiobook.android.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.ServiceLoader

object PlayerManifests {

  private val log = LoggerFactory.getLogger(PlayerManifests::class.java)

  private val providers : MutableList<PlayerManifestParserType>

  init {
    this.providers = ServiceLoader.load(PlayerManifestParserType::class.java).toMutableList()
  }

  private fun findParser(node: ObjectNode) : PlayerManifestParserType? {
    for (provider in this.providers) {
      if (provider.canParse(node)) {
        return provider
      }
    }
    return null
  }

  fun parse(stream: InputStream): PlayerResult<PlayerManifest, Exception> {
    try {
      val mapper = ObjectMapper()
      val result = mapper.readTree(stream)
      if (result is ObjectNode) {
        val provider = this.findParser(result)
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