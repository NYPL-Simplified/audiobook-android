package org.nypl.audiobook.android.open_access

import android.content.Context
import org.nypl.audiobook.android.api.PlayerAudioBookProviderType
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.api.PlayerResult.Failure
import java.util.concurrent.ExecutorService

/**
 * The ExoPlayer implementation of the {@link PlayerAudioBookProviderType} interface.
 */

class ExoAudioBookProvider(
  private val engineExecutor: ExecutorService,
  private val manifest: PlayerManifest)
  : PlayerAudioBookProviderType {

  override fun create(context: Context): PlayerResult<PlayerAudioBookType, Exception> {
    try {
      val parsed = ExoManifest.transform(this.manifest)
      return when (parsed) {
        is PlayerResult.Success ->
          PlayerResult.Success(ExoAudioBook.create(context, engineExecutor, parsed.result))
        is PlayerResult.Failure ->
          Failure(parsed.failure)
      }
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
    }
  }
}