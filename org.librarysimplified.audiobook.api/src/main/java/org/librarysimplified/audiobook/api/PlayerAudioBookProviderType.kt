package org.librarysimplified.audiobook.api

import android.content.Context
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType

/**
 * The interface exposed by audio book providers.
 */

interface PlayerAudioBookProviderType {

  /**
   * Create a new instance of an audio book.
   *
   * @param context An Android context
   * @param extensions The list of extensions that may be used by the provider
   */

  fun create(
    context: Context,
    extensions: List<PlayerExtensionType> = listOf()
  ): PlayerResult<PlayerAudioBookType, Exception>

}