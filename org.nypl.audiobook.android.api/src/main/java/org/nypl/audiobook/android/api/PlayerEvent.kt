package org.nypl.audiobook.android.api

/**
 * The type of events signalled by the player.
 */

sealed class PlayerEvent {

  /**
   * Playback of the given spine element has started.
   */

  data class PlayerEventPlaybackStarted(
    val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback is currently buffering for the given spine element. This can happen at any time
   * during playback if the given spine item has not been downloaded.
   */

  data class PlayerEventPlaybackBuffering(
    val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * The given spine item is playing, and this event is a progress update indicating how far
   * along playback is.
   */

  data class PlayerEventPlaybackProgressUpdate(
    val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback of the given spine element has just completed, and playback will continue to the
   * next spine item if it is available (downloaded).
   */

  data class PlayerEventChapterCompleted(
    val spineElement: PlayerSpineElementType)
    : PlayerEvent()

  /**
   * The player is currently waiting for the given spine element to become available before
   * playback can continue.
   */

  data class PlayerEventChapterWaiting(
    val spineElement: PlayerSpineElementType)
    : PlayerEvent()

  /**
   * Playback of the given spine element has paused.
   */

  data class PlayerEventPlaybackPaused(
    val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback of the given spine element has stopped.
   */

  data class PlayerEventPlaybackStopped(
    val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

}
