package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerManifests
import org.librarysimplified.audiobook.api.PlayerResult
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Tests for the {@link org.librarysimplified.audiobook.api.PlayerRawManifest} type.
 */

abstract class PlayerManifestContract {

  abstract fun log(): Logger

  @Test
  fun testEmptyManifest() {
    val stream = ByteArrayInputStream(ByteArray(0))
    val result = PlayerManifests.parse(stream)
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is PlayerResult.Failure)
  }

  @Test
  fun testErrorMinimal_0() {
    val result = PlayerManifests.parse(resource("error_minimal_0.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is PlayerResult.Failure)
  }

  @Test
  fun testOkMinimal_0() {
    val result = PlayerManifests.parse(resource("ok_minimal_0.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is PlayerResult.Success)

    val success: PlayerResult.Success<PlayerManifest, Exception> =
      result as PlayerResult.Success<PlayerManifest, Exception>

    val manifest = success.result

    Assert.assertEquals(3, manifest.spine.size)
    Assert.assertEquals("Track 0", manifest.spine[0].values["title"].toString())
    Assert.assertEquals("100.0", manifest.spine[0].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[0].values["type"].toString())
    Assert.assertEquals("http://www.example.com/0.mp3", manifest.spine[0].values["href"].toString())

    Assert.assertEquals("Track 1", manifest.spine[1].values["title"].toString())
    Assert.assertEquals("200.0", manifest.spine[1].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[1].values["type"].toString())
    Assert.assertEquals("http://www.example.com/1.mp3", manifest.spine[1].values["href"].toString())

    Assert.assertEquals("Track 2", manifest.spine[2].values["title"].toString())
    Assert.assertEquals("300.0", manifest.spine[2].values["duration"].toString())
    Assert.assertEquals("audio/mpeg", manifest.spine[2].values["type"].toString())
    Assert.assertEquals("http://www.example.com/2.mp3", manifest.spine[2].values["href"].toString())

    Assert.assertEquals("title", manifest.metadata.title)
    Assert.assertEquals("urn:id", manifest.metadata.identifier)
  }

  @Test
  fun testOkFlatlandGardeur() {
    val result = PlayerManifests.parse(resource("flatland.audiobook-manifest.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is PlayerResult.Success)

    val success: PlayerResult.Success<PlayerManifest, Exception> =
      result as PlayerResult.Success<PlayerManifest, Exception>

    val manifest = success.result

    Assert.assertEquals(
      "Flatland: A Romance of Many Dimensions",
      manifest.metadata.title)
    Assert.assertEquals(
      "https://librivox.org/flatland-a-romance-of-many-dimensions-by-edwin-abbott-abbott/",
      manifest.metadata.identifier)

    Assert.assertEquals(
      9,
      manifest.spine.size)

    Assert.assertEquals(
      "Part 1, Sections 1 - 3",
      manifest.spine[0].values["title"].toString())
    Assert.assertEquals(
      "Part 1, Sections 4 - 5",
      manifest.spine[1].values["title"].toString())
    Assert.assertEquals(
      "Part 1, Sections 6 - 7",
      manifest.spine[2].values["title"].toString())
    Assert.assertEquals(
      "Part 1, Sections 8 - 10",
      manifest.spine[3].values["title"].toString())
    Assert.assertEquals(
      "Part 1, Sections 11 - 12",
      manifest.spine[4].values["title"].toString())
    Assert.assertEquals(
      "Part 2, Sections 13 - 14",
      manifest.spine[5].values["title"].toString())
    Assert.assertEquals(
      "Part 2, Sections 15 - 17",
      manifest.spine[6].values["title"].toString())
    Assert.assertEquals(
      "Part 2, Sections 18 - 20",
      manifest.spine[7].values["title"].toString())
    Assert.assertEquals(
      "Part 2, Sections 21 - 22",
      manifest.spine[8].values["title"].toString())

    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[0].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[1].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[2].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[3].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[4].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[5].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[6].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[7].values["type"].toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.spine[8].values["type"].toString())

    Assert.assertEquals(
      "1371",
      manifest.spine[0].values["duration"].toString())
    Assert.assertEquals(
      "1669",
      manifest.spine[1].values["duration"].toString())
    Assert.assertEquals(
      "1506",
      manifest.spine[2].values["duration"].toString())
    Assert.assertEquals(
      "1798",
      manifest.spine[3].values["duration"].toString())
    Assert.assertEquals(
      "1225",
      manifest.spine[4].values["duration"].toString())
    Assert.assertEquals(
      "1659",
      manifest.spine[5].values["duration"].toString())
    Assert.assertEquals(
      "2086",
      manifest.spine[6].values["duration"].toString())
    Assert.assertEquals(
      "2662",
      manifest.spine[7].values["duration"].toString())
    Assert.assertEquals(
      "1177",
      manifest.spine[8].values["duration"].toString())

    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3",
      manifest.spine[0].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3",
      manifest.spine[1].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_3_abbott.mp3",
      manifest.spine[2].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_4_abbott.mp3",
      manifest.spine[3].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_5_abbott.mp3",
      manifest.spine[4].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_6_abbott.mp3",
      manifest.spine[5].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_7_abbott.mp3",
      manifest.spine[6].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_8_abbott.mp3",
      manifest.spine[7].values["href"].toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_9_abbott.mp3",
      manifest.spine[8].values["href"].toString())
  }

  private fun resource(name: String): InputStream {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return PlayerManifestContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
