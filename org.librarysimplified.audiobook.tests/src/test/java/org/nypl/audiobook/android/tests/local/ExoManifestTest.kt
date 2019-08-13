package org.nypl.audiobook.android.tests.local

import org.librarysimplified.audiobook.tests.open_access.ExoManifestContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExoManifestTest : ExoManifestContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoManifestTest::class.java)
  }
}