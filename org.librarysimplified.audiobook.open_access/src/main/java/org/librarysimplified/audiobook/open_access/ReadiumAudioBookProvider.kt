package org.librarysimplified.audiobook.open_access

import android.content.Context
import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.readium.navigator.media2.ExperimentalMedia2
import kotlin.time.ExperimentalTime

/**
 * The Readium implementation of the {@link PlayerAudioBookProviderType} interface.
 */

@OptIn(ExperimentalMedia2::class, ExperimentalTime::class)
class ReadiumAudioBookProvider(
  private val manifest: PlayerManifest,
  private val downloadManifest: () -> PlayerManifest
) : PlayerAudioBookProviderType {

  override fun create(
    context: Context,
    extensions: List<PlayerExtensionType>
  ): PlayerResult<PlayerAudioBookType, Exception> {

    val audiobook = ReadiumAudioBook.create(
      context = context,
      manifest = this.manifest,
      downloadManifest = downloadManifest
    )

    return PlayerResult.Success(audiobook)
  }
}
