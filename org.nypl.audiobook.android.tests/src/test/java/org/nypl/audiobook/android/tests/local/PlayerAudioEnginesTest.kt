package org.nypl.audiobook.android.tests.local

import org.nypl.audiobook.android.tests.PlayerAudioEnginesContract
import org.nypl.audiobook.android.tests.PlayerManifestContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tests for the {@link org.nypl.audiobook.android.api.PlayerAudioEngines} type.
 */

class PlayerAudioEnginesTest : PlayerAudioEnginesContract() {

  override fun log(): Logger {
    return LoggerFactory.getLogger(PlayerAudioEnginesTest::class.java)
  }

}
