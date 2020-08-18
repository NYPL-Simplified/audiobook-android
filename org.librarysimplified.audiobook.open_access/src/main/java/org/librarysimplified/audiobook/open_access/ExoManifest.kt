package org.librarysimplified.audiobook.open_access

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
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
            }
          )
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
        item.title
      val type =
        item.type ?: this.OCTET_STREAM
      val uri =
        this.parseURI(item, index)

      return ExoManifestSpineItem(
        title = title,
        part = 0,
        chapter = index,
        type = type,
        uri = uri,
        originalLink = item,
        duration = item.duration
      )
    }

    private fun parseURI(
      link: PlayerManifestLink,
      index: Int
    ): URI {
      return when (link) {
        is PlayerManifestLink.LinkBasic ->
          link.href
        is PlayerManifestLink.LinkTemplated ->
          throw IllegalArgumentException("Spine item $index has a templated 'href' field")
      }
    }
  }
}
