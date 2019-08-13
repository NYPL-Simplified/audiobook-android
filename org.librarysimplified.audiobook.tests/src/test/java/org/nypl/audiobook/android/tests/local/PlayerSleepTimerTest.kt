package org.nypl.audiobook.android.tests.local

import org.librarysimplified.audiobook.api.PlayerSleepTimer
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.tests.PlayerSleepTimerContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tests for the {@link org.librarysimplified.audiobook.api.PlayerSleepTimer} type.
 */

class PlayerSleepTimerTest : PlayerSleepTimerContract() {

  override fun logger(): Logger {
    return LoggerFactory.getLogger(PlayerSleepTimerTest::class.java)
  }

  override fun create(): PlayerSleepTimerType {
    return PlayerSleepTimer.create()
  }

}
