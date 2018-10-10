package org.nypl.audiobook.android.api

/**
 * A request for an audio engine.
 */

data class PlayerAudioEngineRequest(

  /**
   * The book manifest.
   */

  val manifest: PlayerManifest,

  /**
   * A filter for audio engine providers. If the function returns `true`, then the engine provider
   * is included in the list of providers that can service the given request.
   */

  val filter: (PlayerAudioEngineProviderType) -> Boolean = { true },

  /**
   * A provider of downloads for book parts. Depending on the audio engine used, this provider
   * may not actually be used (some audio engines implement their own downloading and don't
   * allow for using a custom implementation).
   */

  val downloadProvider: PlayerDownloadProviderType)
