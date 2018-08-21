package org.nypl.audiobook.android.api

import org.joda.time.Duration

/**
 * The type of sleep timer events.
 */

sealed class PlayerSleepTimerEvent {

  /**
   * The sleep timer is stopped. This is the initial state.
   */

  object PlayerSleepTimerStopped : PlayerSleepTimerEvent()

  /**
   * The sleep timer is currently running. This state will be published frequently while the sleep
   * timer is counting down.
   */

  data class PlayerSleepTimerRunning(
    val remaining: Duration)
    : PlayerSleepTimerEvent()

  /**
   * The user cancelled the sleep timer countdown.
   */

  data class PlayerSleepTimerCancelled(
    val remaining: Duration)
    : PlayerSleepTimerEvent()

  /**
   * The sleep timer ran to completion.
   */

  object PlayerSleepTimerFinished : PlayerSleepTimerEvent()

}
