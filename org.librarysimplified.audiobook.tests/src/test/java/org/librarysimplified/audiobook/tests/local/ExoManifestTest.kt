package org.librarysimplified.audiobook.tests.local

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExoManifestTest : ExoManifestContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoManifestTest::class.java)
  }
}
