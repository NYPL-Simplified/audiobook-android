package org.nypl.audiobook.android.tests.device

import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.audiobook.android.api.PlayerSleepTimer
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.tests.PlayerSleepTimerContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerSleepTimerTest : PlayerSleepTimerContract() {
  override fun create(): PlayerSleepTimerType {
    return PlayerSleepTimer.create()
  }

  override fun logger(): Logger {
    return LoggerFactory.getLogger(PlayerSleepTimerTest::class.java)
  }
}
