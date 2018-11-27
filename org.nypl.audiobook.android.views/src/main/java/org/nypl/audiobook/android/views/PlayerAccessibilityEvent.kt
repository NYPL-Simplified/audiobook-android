package org.nypl.audiobook.android.views

/**
 * The type of events significant to accessibility.
 */

sealed class PlayerAccessibilityEvent {

  /**
   * A localized accessibility message suitable for direct use with a screen reader.
   */

  abstract val message: String

  /**
   * The player has been buffering for a length of time significant enough to warrant
   * announcing to the user.
   */

  data class PlayerAccessibilityIsBuffering(
    override val message: String) : PlayerAccessibilityEvent()

  /**
   * The player cannot continue until the target chapter has been downloaded.
   */

  data class PlayerAccessibilityIsWaitingForChapter(
    override val message: String) : PlayerAccessibilityEvent()

  /**
   * The player has published an error significant enough to warrant announcing to the user.
   */

  data class PlayerAccessibilityErrorOccurred(
    override val message: String) : PlayerAccessibilityEvent()

}
