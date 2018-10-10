package org.nypl.audiobook.android.tests.device

import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.audiobook.android.tests.PlayerManifestContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerManifestTest : PlayerManifestContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(PlayerManifestTest::class.java)
  }
}
