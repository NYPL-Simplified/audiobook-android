package org.librarysimplified.audiobook.tests.local

import android.content.Context
import org.librarysimplified.audiobook.tests.open_access.ExoEngineProviderContract
import org.mockito.Mockito
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExoEngineProviderTest : ExoEngineProviderContract() {

  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoEngineProviderTest::class.java)
  }

  override fun context(): Context {
    val context = Mockito.mock(Context::class.java)
    return context
  }

  override fun onRealDevice(): Boolean {
    return false
  }
}
