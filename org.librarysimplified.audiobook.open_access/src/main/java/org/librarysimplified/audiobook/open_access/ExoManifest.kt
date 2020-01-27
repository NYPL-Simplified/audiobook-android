package org.librarysimplified.audiobook.open_access

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.librarysimplified.audiobook.api.PlayerResult
import java.net.URI

/**
 * A manifest transformed such that it contains information relevant to the Exo audio engine.
 */

data class ExoManifest(
  val title: String,
  val id: String,
  val spineItems: List<ExoManifestSpineItem>
) {

  companion object {

    /**
     * Parse an ExoPlayer manifest from the given raw manifest.
     */

    fun transform(manifest: PlayerManifest): PlayerResult<ExoManifest, Exception> {
      try {
        return PlayerResult.Success(
          ExoManifest(
            manifest.metadata.title,
            manifest.metadata.identifier,
            manifest.readingOrder.mapIndexed { index, item ->
              this.processSpineItem(index, item)
            })
        )
      } catch (e: Exception) {
        return PlayerResult.Failure(e)
      }
    }

    private val OCTET_STREAM =
      MIMEType("application", "octet-stream", mapOf())

    private fun processSpineItem(
      index: Int,
      item: PlayerManifestLink
    ): ExoManifestSpineItem {

      val title =
        item.title ?: index.toString()
      val type =
        item.type ?: this.OCTET_STREAM
      val uri =
        this.parseURI(item, index)
      val duration =
        this.parseDuration(item, index)

      return ExoManifestSpineItem(
        title = title,
        part = 0,
        chapter = index,
        type = type,
        duration = duration,
        uri = uri
      )
    }

    private fun parseDuration(
      link: PlayerManifestLink,
      index: Int
    ): Double {
      return when (val duration = link.duration) {
        null ->
          throw IllegalArgumentException(
            "Spine item ${index} is missing the required 'duration' field"
          )
        else ->
          duration
      }
    }

    private fun parseURI(
      link: PlayerManifestLink,
      index: Int
    ): URI {
      return when (link) {
        is PlayerManifestLink.LinkBasic ->
          link.href
        is PlayerManifestLink.LinkTemplated ->
          throw IllegalArgumentException("Spine item ${index} has a templated 'href' field")
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
  val type: MIMEType,
  val duration: Double,
  val uri: URI
)
