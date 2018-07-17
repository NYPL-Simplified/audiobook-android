package org.nypl.audiobook.android.api

/**
 * The version of an engine provider.
 */

data class PlayerAudioEngineVersion(
  val major : Int,
  val minor : Int,
  val patch : Int)
