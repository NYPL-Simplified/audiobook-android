package org.nypl.audiobook.android.open_access

import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerResult
import java.net.URI

/**
 * A manifest transformed such that it contains information relevant to the Exo audio engine.
 */

data class ExoManifest(
  val title: String,
  val id: String,
  val spineItems: List<ExoManifestSpineItem>) {

  companion object {
    fun transform(manifest: PlayerManifest): PlayerResult<ExoManifest, Exception> {
      return PlayerResult.Failure(Exception("Not implemented!"))
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
