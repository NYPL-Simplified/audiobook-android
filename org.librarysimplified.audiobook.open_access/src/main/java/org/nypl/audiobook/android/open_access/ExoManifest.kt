package org.nypl.audiobook.android.open_access

import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerManifestScalar
import org.librarysimplified.audiobook.api.PlayerManifestScalar.PlayerManifestScalarNumber
import org.librarysimplified.audiobook.api.PlayerManifestSpineItem
import org.librarysimplified.audiobook.api.PlayerResult
import java.net.URI

/**
 * A manifest transformed such that it contains information relevant to the Exo audio engine.
 */

data class ExoManifest(
  val title: String,
  val id: String,
  val spineItems: List<ExoManifestSpineItem>) {

  companion object {

    /**
     * Parse an ExoPlayer manifest from the given raw manifest.
     */

    fun transform(manifest: PlayerManifest): PlayerResult<ExoManifest, Exception> {
      try {
        return PlayerResult.Success(ExoManifest(
          manifest.metadata.title,
          manifest.metadata.identifier,
          manifest.spine.mapIndexed { index, item -> processSpineItem(index, item) }))
      } catch (e: Exception) {
        return PlayerResult.Failure(e)
      }
    }

    private fun processSpineItem(
      index: Int,
      item: PlayerManifestSpineItem): ExoManifestSpineItem {

      val values = item.values
      val title = parseTitle(values, index)
      val type = parseType(values)
      val uri = parseURI(values, index)
      val duration = parseDuration(values, index)

      return ExoManifestSpineItem(
        title = title,
        part = 0,
        chapter = index,
        type = type,
        duration = duration,
        uri = uri)
    }

    private fun parseDuration(values: Map<String, PlayerManifestScalar>, index: Int): Double {
      if (values.containsKey("duration")) {
        val value = values["duration"]
        return when (value) {
          is PlayerManifestScalar.PlayerManifestScalarString ->
            throw IllegalArgumentException("Spine item ${index} has an invalid 'duration' field")
          is PlayerManifestScalar.PlayerManifestScalarBoolean ->
            throw IllegalArgumentException("Spine item ${index} has an invalid 'duration' field")
          is PlayerManifestScalarNumber.PlayerManifestScalarReal ->
            value.number
          is PlayerManifestScalarNumber.PlayerManifestScalarInteger ->
            value.number.toDouble()
          null ->
            throw IllegalArgumentException(
              "Spine item ${index} is missing the required 'duration' field")
        }
      }
      throw IllegalArgumentException("Spine item ${index} is missing the required 'duration' field")
    }

    private fun parseURI(values: Map<String, PlayerManifestScalar>, index: Int): URI {
      if (values.containsKey("href")) {
        return URI(values["href"].toString())
      }
      throw IllegalArgumentException("Spine item ${index} is missing the required 'href' field")
    }

    private fun parseType(values: Map<String, PlayerManifestScalar>): String {
      return if (values.containsKey("type")) {
        return values["type"].toString()
      } else {
        "application/octet-stream"
      }
    }

    private fun parseTitle(values: Map<String, PlayerManifestScalar>, index: Int): String {
      return if (values.containsKey("title")) {
        values["title"].toString()
      } else {
        index.toString()
      }
    }
  }
}

/**
 * A spine item transformed to expose the information critical to the ExoPlayer engine.
 */

data class ExoManifestSpineItem(
  val title: String,
  val part: Int,
  val chapter: Int,
  val type: String,
  val duration: Double,
  val uri: URI)
