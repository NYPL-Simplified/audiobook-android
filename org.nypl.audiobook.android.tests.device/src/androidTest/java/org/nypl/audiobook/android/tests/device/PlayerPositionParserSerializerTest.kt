package org.nypl.audiobook.android.tests.device

import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4
import org.junit.runner.RunWith
import org.nypl.audiobook.android.api.PlayerPositionParserType
import org.nypl.audiobook.android.api.PlayerPositionSerializerType
import org.nypl.audiobook.android.api.PlayerPositions
import org.nypl.audiobook.android.tests.PlayerPositionParserSerializerContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerPositionParserSerializerTest : PlayerPositionParserSerializerContract() {

  override fun logger(): Logger {
    return LoggerFactory.getLogger(PlayerPositionParserSerializerTest::class.java)
  }

  override fun createParser(): PlayerPositionParserType {
    return PlayerPositions
  }

  override fun createSerializer(): PlayerPositionSerializerType {
    return PlayerPositions
  }

}