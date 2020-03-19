package org.librarysimplified.audiobook.tests.local

import org.librarysimplified.audiobook.tests.FeedbooksRightsCheckContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FeedbooksRightsCheckTest : FeedbooksRightsCheckContract() {

  override fun log(): Logger {
    return LoggerFactory.getLogger(FeedbooksRightsCheckTest::class.java)
  }
}
