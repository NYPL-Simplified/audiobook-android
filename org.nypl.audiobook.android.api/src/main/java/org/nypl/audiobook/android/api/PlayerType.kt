package org.nypl.audiobook.android.api

import rx.Observable

/**
 * A player for a book.
 */

interface PlayerType : AutoCloseable {

  /**
   * Close this audio book player. All subsequent method calls on this player will throw
   * {@link java.lang.IllegalStateException} indicating that the player is closed.
   */

  @Throws(java.lang.IllegalStateException::class)
  override fun close()

  /**
   * True if the player is currently playing.
   */

  val isPlaying: Boolean

  /**
   * The playback rate for the player.
   */

  var playbackRate: PlayerPlaybackRate

  /**
   * @return `true` if and only if {@link #close()} has been called on this player
   */

  val isClosed: Boolean

  /**
   * An observable that publishes player status updates.
   */

  val events: Observable<PlayerEvent>

  /**
   * Play at current playhead location
   */

  fun play()

  /**
   * Pause playback
   */

  fun pause()

  /**
   * Skip to the next chapter.
   */

  fun skipToNextChapter()

  /**
   * Skip to the previous chapter.
   */

  fun skipToPreviousChapter()

  /**
   * Skip forward 15 seconds and start playback
   */

  fun skipForward()

  /**
   * Skip back 15 seconds and start playback
   */

  fun skipBack()

  /**
   * Move playhead and immediately start playing. This method is useful for scenarios like a table
   * of contents where you select a new chapter and wish to immediately start playback.
   */

  fun playAtLocation(
    location: PlayerPosition)

  /**
   * Move playhead but do not start playback. This is useful for state restoration where we want
   * to prepare for playback at a specific point, but playback has not yet been requested.
   */

  fun movePlayheadToLocation(
    location: PlayerPosition)
}