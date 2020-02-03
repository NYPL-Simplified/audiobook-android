package org.librarysimplified.audiobook.open_access

import android.content.Context
import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerResult.Failure
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import java.util.concurrent.ScheduledExecutorService

/**
 * The ExoPlayer implementation of the {@link PlayerAudioBookProviderType} interface.
 */

class ExoAudioBookProvider(
  private val engineExecutor: ScheduledExecutorService,
  private val downloadProvider: PlayerDownloadProviderType,
  private val manifest: PlayerManifest,
  private val engineProvider: ExoEngineProvider
) : PlayerAudioBookProviderType {

  override fun create(
    context: Context,
    extensions: List<PlayerExtensionType>
  ): PlayerResult<PlayerAudioBookType, Exception> {
    try {
      return when (val parsed = ExoManifest.transform(this.manifest)) {
        is PlayerResult.Success ->
          PlayerResult.Success(
            ExoAudioBook.create(
              context = context,
              downloadProvider = this.downloadProvider,
              engineExecutor = this.engineExecutor,
              engineProvider = this.engineProvider,
              extensions = extensions,
              manifest = parsed.result
            )
          )
        is Failure ->
          Failure(parsed.failure)
      }
    } catch (e: Exception) {
      return Failure(e)
    }
  }
}
