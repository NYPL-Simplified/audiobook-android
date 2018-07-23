package org.nypl.audiobook.android.api

import android.content.Context

/**
 * The interface exposed by audio book providers.
 */

interface PlayerAudioBookProviderType {

  /**
   * Create a new instance of an audio book.
   *
   * @param context An Android context
   */

  fun create(context: Context): PlayerResult<PlayerAudioBookType, Exception>

}