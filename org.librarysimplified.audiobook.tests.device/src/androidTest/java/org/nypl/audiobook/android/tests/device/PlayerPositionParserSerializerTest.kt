package org.nypl.audiobook.android.tests.device

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.runner.RunWith
import org.librarysimplified.audiobook.api.PlayerPositionParserType
import org.librarysimplified.audiobook.api.PlayerPositionSerializerType
import org.librarysimplified.audiobook.api.PlayerPositions
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