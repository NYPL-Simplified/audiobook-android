package org.librarysimplified.audiobook.tests.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith
import org.librarysimplified.audiobook.tests.open_access.ExoManifestContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ExoManifestTest : ExoManifestContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoManifestTest::class.java)
  }
}