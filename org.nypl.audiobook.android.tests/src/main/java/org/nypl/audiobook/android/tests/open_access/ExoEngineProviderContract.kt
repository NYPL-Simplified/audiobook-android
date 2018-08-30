package org.nypl.audiobook.android.tests.open_access

import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.nypl.audiobook.android.api.PlayerAudioEngineRequest
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifests
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.open_access.ExoEngineProvider
import org.nypl.audiobook.android.tests.DishonestDownloadProvider
import org.slf4j.Logger
import java.io.InputStream

/**
 * Tests for the {@link org.nypl.audiobook.android.open_access.ExoEngineProvider} type.
 */

abstract class ExoEngineProviderContract {

  abstract fun log(): Logger

  abstract fun context(): Context

  @Test
  fun testAudioEnginesTrivial() {
    val manifest = parseManifest("ok_minimal_0.json")
    val request = PlayerAudioEngineRequest(manifest, filter = { true }, downloadProvider = DishonestDownloadProvider())
    val engine_provider = ExoEngineProvider()
    val book_provider = engine_provider.tryRequest(request)
    Assert.assertNotNull("Engine must handle manifest", book_provider)
    val book_provider_nn = book_provider!!
    val result = book_provider_nn.create(context())
    this.log().debug("testAudioEnginesTrivial:result: {}", result)
    Assert.assertTrue("Engine accepts book", result is PlayerResult.Success)
  }

  private fun parseManifest(file: String): PlayerManifest {
    val result = PlayerManifests.parse(resource(file))
    this.log().debug("parseManifest: result: {}", result)
    Assert.assertTrue("Result is success", result is PlayerResult.Success)
    val manifest = (result as PlayerResult.Success).result
    return manifest
  }

  private fun resource(name: String): InputStream {
    val path = "/org/nypl/audiobook/android/tests/" + name
    return ExoEngineProviderContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
