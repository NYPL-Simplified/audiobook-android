package org.librarysimplified.audiobook.player.api

import android.content.Context
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.readium.r2.shared.publication.Publication

/**
 * A request for an audio engine.
 */

data class PlayerAudioEngineRequest(

  val context: Context,

  val publication: Publication,

  val bookID: PlayerBookID,

  /**
   * The book manifest.
   */

  val manifest: PlayerManifest,

  val downloadManifest: (() -> PlayerManifest)?,

  /**
   * The user agent used to make HTTP requests.
   */

  val userAgent: PlayerUserAgent,

  /**
   * A filter for audio engine providers. If the function returns `true`, then the engine provider
   * is included in the list of providers that can service the given request.
   */

  val filter: (PlayerAudioEngineProviderType) -> Boolean =
    {
      true
    },
)
