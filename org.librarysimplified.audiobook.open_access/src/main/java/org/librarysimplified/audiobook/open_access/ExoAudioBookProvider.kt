package org.librarysimplified.audiobook.open_access

import android.content.Context
import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerResult.Failure
import org.librarysimplified.audiobook.api.PlayerUserAgent
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
  private val engineProvider: ExoEngineProvider,
  private val userAgent: PlayerUserAgent
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
              engineProvider = this.engineProvider,
              context = context,
              engineExecutor = this.engineExecutor,
              manifest = parsed.result,
              downloadProvider = this.downloadProvider,
              extensions = extensions,
              userAgent = this.userAgent
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
