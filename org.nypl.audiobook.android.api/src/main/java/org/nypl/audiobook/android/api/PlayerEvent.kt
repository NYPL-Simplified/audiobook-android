package org.nypl.audiobook.android.api

/**
 * The type of events signalled by the player.
 */

sealed class PlayerEvent {

  /**
   * The spine element to which this event refers.
   */

  abstract val spineElement: PlayerSpineElementType

  /**
   * Playback of the given spine element has started.
   */

  data class PlayerEventPlaybackStarted(
    override val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback is currently buffering for the given spine element. This can happen at any time
   * during playback if the given spine item has not been downloaded.
   */

  data class PlayerEventPlaybackBuffering(
    override val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * The given spine item is playing, and this event is a progress update indicating how far
   * along playback is.
   */

  data class PlayerEventPlaybackProgressUpdate(
    override val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback of the given spine element has just completed, and playback will continue to the
   * next spine item if it is available (downloaded).
   */

  data class PlayerEventChapterCompleted(
    override val spineElement: PlayerSpineElementType)
    : PlayerEvent()

  /**
   * The player is currently waiting for the given spine element to become available before
   * playback can continue.
   */

  data class PlayerEventChapterWaiting(
    override val spineElement: PlayerSpineElementType)
    : PlayerEvent()

  /**
   * Playback of the given spine element has paused.
   */

  data class PlayerEventPlaybackPaused(
    override val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

  /**
   * Playback of the given spine element has stopped.
   */

  data class PlayerEventPlaybackStopped(
    override val spineElement: PlayerSpineElementType,
    val offsetMilliseconds: Int)
    : PlayerEvent()

}
