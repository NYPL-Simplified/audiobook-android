package org.nypl.audiobook.android.tests

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerResult
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Tests for the {@link org.nypl.audiobook.android.api.PlayerRawManifest} type.
 */

abstract class PlayerManifestContract {

  abstract fun log() : Logger

  @Test
  fun testEmptyManifest() {
    val stream = ByteArrayInputStream(ByteArray(0))
    val result = PlayerManifest.parse(stream)
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is PlayerResult.Failure)
  }

  @Test
  fun testErrorMinimal_0() {
    val result = PlayerManifest.parse(resource("error_minimal_0.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is PlayerResult.Failure)
  }

  @Test
  fun testOkMinimal_0() {
    val result = PlayerManifest.parse(resource("ok_minimal_0.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is PlayerResult.Success)

    val success : PlayerResult.Success<PlayerManifest, Exception> =
      result as PlayerResult.Success<PlayerManifest, Exception>

    val manifest = success.result
    Assert.assertEquals(3, manifest.links.size)
    Assert.assertEquals("http://example.org/a", manifest.links[0].href.toString())
    Assert.assertEquals("something-0", manifest.links[0].relation)
    Assert.assertEquals("http://example.org/b", manifest.links[1].href.toString())
    Assert.assertEquals("something-1", manifest.links[1].relation)
    Assert.assertEquals("http://example.org/c", manifest.links[2].href.toString())
    Assert.assertEquals("something-2", manifest.links[2].relation)

    Assert.assertEquals(3, manifest.spine.size)
    Assert.assertEquals("Track 0", manifest.spine[0].values["title"].toString())
    Assert.assertEquals("100.0", manifest.spine[0].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[0].values["type"].toString())

    Assert.assertEquals("Track 1", manifest.spine[1].values["title"].toString())
    Assert.assertEquals("200.0", manifest.spine[1].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[1].values["type"].toString())

    Assert.assertEquals("Track 2", manifest.spine[2].values["title"].toString())
    Assert.assertEquals("300.0", manifest.spine[2].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[2].values["type"].toString())

    Assert.assertEquals("title", manifest.metadata.title)
    Assert.assertEquals("urn:id", manifest.metadata.identifier)
    Assert.assertEquals("en", manifest.metadata.language)
    Assert.assertEquals(1000.0, manifest.metadata.duration, 0.0)

    Assert.assertEquals(3, manifest.metadata.authors.size)
    Assert.assertEquals("author_0", manifest.metadata.authors[0])
    Assert.assertEquals("author_1", manifest.metadata.authors[1])
    Assert.assertEquals("author_2", manifest.metadata.authors[2])
  }

  private fun resource(name: String): InputStream {
    val path = "/org/nypl/audiobook/android/tests/" + name
    return PlayerManifestContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
