package org.librarysimplified.audiobook.api

import io.reactivex.Observable

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
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  val isPlaying: Boolean

  /**
   * The playback rate for the player.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  var playbackRate: PlayerPlaybackRate

  /**
   * @return `true` if and only if {@link #close()} has been called on this player
   */

  val isClosed: Boolean

  /**
   * An observable that publishes player status updates.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  val events: Observable<PlayerEvent>

  /**
   * Play at current playhead location
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun play()

  /**
   * Pause playback
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun pause()

  /**
   * Skip to the next chapter.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun skipToNextChapter()

  /**
   * Skip to the previous chapter.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun skipToPreviousChapter()

  /**
   * Skip forwards/backwards, possibly across chapter boundaries. If the given parameter is
   * positive, skip forwards. If the given parameter is negative, skip backwards. If the given
   * parameter is `0`, do nothing.
   *
   * Note: Implementations are not required to support skipping over multiple chapters in a
   * single skip. Please use the explicit `playAtLocation` API if you want to perform large
   * jumps.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun skipPlayhead(milliseconds: Long)

  /**
   * Skip forward 15 seconds and start playback
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun skipForward() {
    this.skipPlayhead(15_000L)
  }

  /**
   * Skip back 15 seconds and start playback
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun skipBack() {
    this.skipPlayhead(-15_000L)
  }

  /**
   * Move playhead and immediately start playing. This method is useful for scenarios like a table
   * of contents where you select a new chapter and wish to immediately start playback.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun playAtLocation(
    location: PlayerPosition
  )

  /**
   * Move playhead but do not start playback. This is useful for state restoration where we want
   * to prepare for playback at a specific point, but playback has not yet been requested.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun movePlayheadToLocation(
    location: PlayerPosition
  )

  /**
   * Move playhead to the start of the book and immediately start playing.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun playAtBookStart()

  /**
   * Move playhead to the start of the book but do not start playback.
   *
   * @throws java.lang.IllegalStateException If and only if the player is closed
   */

  fun movePlayheadToBookStart()
}
