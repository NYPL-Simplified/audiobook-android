package org.librarysimplified.audiobook.tests.device

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.MediumTest
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Before
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class ExoPlayerEngineProviderTest : ReadiumEngineProviderContract() {

  private var instrumentationContext: Context? = null

  @Before
  override fun setup() {
    super.setup()
    this.instrumentationContext = InstrumentationRegistry.getInstrumentation().context
  }

  override fun log(): Logger {
    return LoggerFactory.getLogger(ExoPlayerEngineProviderTest::class.java)
  }

  override fun context(): Context {
    return this.instrumentationContext!!
  }

  override fun onRealDevice(): Boolean {
    return true
  }
}
