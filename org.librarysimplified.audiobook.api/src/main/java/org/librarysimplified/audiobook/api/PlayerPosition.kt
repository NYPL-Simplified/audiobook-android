package org.librarysimplified.audiobook.api

/**
 * The playback position of the player.
 */

data class PlayerPosition(
  val title: String?,
  val part: Int,
  val chapter: Int,
  val offsetMilliseconds: Long
)
