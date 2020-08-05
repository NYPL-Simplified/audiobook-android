package org.librarysimplified.audiobook.tests.local

import org.librarysimplified.audiobook.tests.FeedbooksSignatureCheckContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FeedbooksSignatureCheckTest : FeedbooksSignatureCheckContract() {
  override fun log(): Logger {
    return LoggerFactory.getLogger(FeedbooksSignatureCheckTest::class.java)
  }
}
