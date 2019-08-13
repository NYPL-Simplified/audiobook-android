package org.nypl.audiobook.android.tests.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith
import org.librarysimplified.audiobook.tests.PlayerAudioEnginesContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerAudioEnginesTest : PlayerAudioEnginesContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(PlayerAudioEnginesTest::class.java)
  }
}
