package org.nypl.audiobook.android.api

/**
 * The playback rate of the player.
 */

enum class PlayerPlaybackRate(val speed: Double) {

  /**
   * 75% speed.
   */

  THREE_QUARTERS_TIME(0.75),

  /**
   * Normal speed.
   */

  NORMAL_TIME(1.0),

  /**
   * 125% speed.
   */

  ONE_AND_A_QUARTER_TIME(1.25),

  /**
   * 150% speed.
   */

  ONE_AND_A_HALF_TIME(1.50),

  /**
   * 200% speed.
   */

  DOUBLE_TIME(2.0);
}