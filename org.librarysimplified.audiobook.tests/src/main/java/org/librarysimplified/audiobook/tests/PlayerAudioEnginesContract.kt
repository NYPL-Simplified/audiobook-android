package org.librarysimplified.audiobook.tests

import android.content.Context
import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.player.api.PlayerAudioEngines
import org.librarysimplified.audiobook.player.api.PlayerBookID
import org.mockito.Mockito
import org.readium.r2.shared.publication.Publication
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
    val context = Mockito.mock(Context::class.java)
    val pubication = Mockito.mock(Publication::class.java)
    val request = PlayerAudioEngineRequest(
      context = context,
      bookID = PlayerBookID.transform("dummy"),
      publication = pubication,
      manifest = manifest,
      filter = { true },
      userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0")
    )
    val provider = PlayerAudioEngines.findBestFor(request)
    Assert.assertNotNull("Exactly one open access provider should be present", provider)
  }

  @Test
  fun testAudioEnginesAllFiltered() {
    val manifest = parseManifest("ok_minimal_0.json")
    val context = Mockito.mock(Context::class.java)
    val pubication = Mockito.mock(Publication::class.java)
    val request = PlayerAudioEngineRequest(
      context = context,
      bookID = PlayerBookID.transform("dummy"),
      publication = pubication,
      manifest = manifest,
      filter = { false },
      userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0")
    )
    val provider = PlayerAudioEngines.findBestFor(request)
    Assert.assertNull("No providers should be present",  provider)
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
