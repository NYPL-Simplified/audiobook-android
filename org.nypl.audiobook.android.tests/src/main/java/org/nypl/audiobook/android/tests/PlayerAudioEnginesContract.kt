package org.nypl.audiobook.android.tests

import org.junit.Assert
import org.junit.Test
import org.nypl.audiobook.android.api.PlayerAudioEngineRequest
import org.nypl.audiobook.android.api.PlayerAudioEngines
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifests
import org.nypl.audiobook.android.api.PlayerResult
import org.slf4j.Logger
import java.io.InputStream

/**
 * Tests for the {@link org.nypl.audiobook.android.api.PlayerAudioEngines} type.
 */

abstract class PlayerAudioEnginesContract {

  abstract fun log(): Logger

  @Test
  fun testAudioEnginesTrivial() {
    val manifest = parseManifest("ok_minimal_0.json")
    val request = PlayerAudioEngineRequest(manifest, { true }, DishonestDownloadProvider())
    val providers = PlayerAudioEngines.findAllFor(request)
    Assert.assertEquals("Exactly one open access provider should be present", 1, providers.size)
  }

  @Test
  fun testAudioEnginesAllFiltered() {
    val manifest = parseManifest("ok_minimal_0.json")
    val request = PlayerAudioEngineRequest(manifest, { false }, DishonestDownloadProvider())
    val providers = PlayerAudioEngines.findAllFor(request)
    Assert.assertEquals("No providers should be present", 0, providers.size)
  }

  private fun parseManifest(file: String): PlayerManifest {
    val result = PlayerManifests.parse(resource(file))
    this.log().debug("result: {}", result)
    Assert.assertTrue("Result is success", result is PlayerResult.Success)
    val manifest = (result as PlayerResult.Success).result
    return manifest
  }

  private fun resource(name: String): InputStream {
    val path = "/org/nypl/audiobook/android/tests/" + name
    return PlayerAudioEnginesContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
