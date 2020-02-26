package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerAudioEngines
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.Logger
import java.net.URI

/**
 * Tests for the {@link org.librarysimplified.audiobook.api.PlayerAudioEngines} type.
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
    val result =
      ManifestParsers.parse(
        uri = URI.create("urn:$file"),
        streams = resource(file),
        extensions = listOf()
      )

    this.log().debug("result: {}", result)
    Assert.assertTrue("Result is success", result is ParseResult.Success)
    val manifest = (result as ParseResult.Success).result
    return manifest
  }

  private fun resource(name: String): ByteArray {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return PlayerAudioEnginesContract::class.java.getResourceAsStream(path)?.readBytes()
      ?: throw AssertionError("Missing resource file: " + path)
  }
}
