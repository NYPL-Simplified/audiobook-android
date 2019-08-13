package org.nypl.audiobook.android.tests.device

import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.audiobook.android.tests.open_access.ExoManifestContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ExoManifestTest : ExoManifestContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoManifestTest::class.java)
  }
}