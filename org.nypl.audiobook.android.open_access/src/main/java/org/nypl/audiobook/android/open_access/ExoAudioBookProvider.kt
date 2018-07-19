package org.nypl.audiobook.android.open_access

import org.nypl.audiobook.android.api.PlayerAudioBookProviderType
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.api.PlayerResult.Failure

/**
 * The ExoPlayer implementation of the {@link PlayerAudioBookProviderType} interface.
 */

internal class ExoAudioBookProvider(
  private val manifest: PlayerManifest)
  : PlayerAudioBookProviderType {

  override fun create(): PlayerResult<PlayerAudioBookType, Exception> {
    return Failure(Exception("Not implemented!"))
  }
}