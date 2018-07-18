package org.nypl.audiobook.android.api

/**
 * An engine and book provider.
 */

data class PlayerEngineAndBook(
  val engine : PlayerAudioEngineProviderType,
  val book : PlayerAudioBookProviderType)