package org.nypl.audiobook.android.tests.device

import android.content.Context
import android.support.test.filters.MediumTest
import android.support.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.audiobook.android.tests.open_access.ExoEngineProviderContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ExoEngineProviderTest : ExoEngineProviderContract() {

  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoEngineProviderTest::class.java)
  }

  override fun context(): Context {
    TODO("not implemented")
  }
}
