package org.librarysimplified.audiobook.views

/**
 * Configuration values for the sleep timer.
 */

enum class PlayerSleepTimerConfiguration {

  /**
   * The sleep timer will finish now. This option is primarily useful for debugging.
   */

  NOW,

  /**
   * The sleep timer will never finish. This is essentially used to switch off the sleep timer.
   */

  OFF,

  /**
   * The sleep timer will finish in 15 minutes.
   */

  MINUTES_15,

  /**
   * The sleep timer will finish in 30 minutes.
   */

  MINUTES_30,

  /**
   * The sleep timer will finish in 45 minutes.
   */

  MINUTES_45,

  /**
   * The sleep timer will finish in 60 minutes.
   */

  MINUTES_60,

  /**
   * The sleep timer will finish at the end of the current chapter.
   */

  END_OF_CHAPTER,
}
