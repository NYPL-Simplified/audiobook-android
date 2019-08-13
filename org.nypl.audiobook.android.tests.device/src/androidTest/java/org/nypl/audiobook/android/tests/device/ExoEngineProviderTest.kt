package org.nypl.audiobook.android.tests.device

import android.content.Context
import androidx.test.InstrumentationRegistry
import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4

import org.junit.Before
import org.junit.runner.RunWith
import org.nypl.audiobook.android.tests.open_access.ExoEngineProviderContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ExoEngineProviderTest : ExoEngineProviderContract() {

  private var instrumentationContext: Context? = null

  @Before
  override fun setup() {
    super.setup()
    this.instrumentationContext = InstrumentationRegistry.getContext()
  }

  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoEngineProviderTest::class.java)
  }

  override fun context(): Context {
    return this.instrumentationContext!!
  }
}
