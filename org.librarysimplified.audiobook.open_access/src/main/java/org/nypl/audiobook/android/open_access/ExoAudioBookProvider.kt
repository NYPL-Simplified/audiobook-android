package org.nypl.audiobook.android.open_access

import android.content.Context
import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerResult.Failure
import java.util.concurrent.ScheduledExecutorService

/**
 * The ExoPlayer implementation of the {@link PlayerAudioBookProviderType} interface.
 */

class ExoAudioBookProvider(
  private val engineExecutor: ScheduledExecutorService,
  private val downloadProvider: PlayerDownloadProviderType,
  private val manifest: PlayerManifest,
  private val engineProvider: ExoEngineProvider)
  : PlayerAudioBookProviderType {

  override fun create(context: Context): PlayerResult<PlayerAudioBookType, Exception> {
    try {
      val parsed = ExoManifest.transform(this.manifest)
      return when (parsed) {
        is PlayerResult.Success ->
          PlayerResult.Success(
            ExoAudioBook.create(
              engineProvider = this.engineProvider,
              context = context,
              engineExecutor = this.engineExecutor,
              manifest = parsed.result,
              downloadProvider = this.downloadProvider))
        is PlayerResult.Failure ->
          Failure(parsed.failure)
      }
    } catch (e: Exception) {
      return PlayerResult.Failure(e)
    }
  }
}