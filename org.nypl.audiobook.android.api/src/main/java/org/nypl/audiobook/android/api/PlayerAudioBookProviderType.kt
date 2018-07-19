package org.nypl.audiobook.android.api

/**
 * The interface exposed by audio book providers.
 */

interface PlayerAudioBookProviderType {

  /**
   * Create a new instance of an audio book.
   */

  fun create(): PlayerResult<PlayerAudioBookType, Exception>

}