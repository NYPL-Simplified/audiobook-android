package org.nypl.audiobook.android.tests.local

import org.nypl.audiobook.android.api.PlayerSleepTimer
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.tests.PlayerSleepTimerContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tests for the {@link org.nypl.audiobook.android.api.PlayerSleepTimer} type.
 */

class PlayerSleepTimerTest : PlayerSleepTimerContract() {

  override fun logger(): Logger {
    return LoggerFactory.getLogger(PlayerSleepTimerTest::class.java)
  }

  override fun create(): PlayerSleepTimerType {
    return PlayerSleepTimer.create()
  }

}
