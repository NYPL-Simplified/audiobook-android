package org.nypl.audiobook.android.tests.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith
import org.librarysimplified.audiobook.api.PlayerSleepTimer
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.tests.PlayerSleepTimerContract
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
