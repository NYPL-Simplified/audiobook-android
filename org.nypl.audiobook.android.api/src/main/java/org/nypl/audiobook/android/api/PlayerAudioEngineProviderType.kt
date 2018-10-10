package org.nypl.audiobook.android.api

/**
 * The interface exposed by audio engine providers.
 */

interface PlayerAudioEngineProviderType {

  /**
   * @return The name of the audio engine provider as a reverse-DNS style name
   */

  fun name(): String

  /**
   * @return The version of the audio engine provider
   */

  fun version(): PlayerAudioEngineVersion

  /**
   * Determine if the given manifest refers to a book that can be played by this audio engine
   * provider. If the book can be supported, the returned value allows for constructing the
   * actual book instance. If the book cannot be supported, the function returns `null`.
   *
   * @return An audio book provider, if the book is supported by the engine
   */

  fun tryRequest(request: PlayerAudioEngineRequest): PlayerAudioBookProviderType?

}