package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerManifestScalar
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

/**
 * Tests for the {@link org.librarysimplified.audiobook.api.PlayerRawManifest} type.
 */

abstract class PlayerManifestContract {

  abstract fun log(): Logger

  @Test
  fun testEmptyManifest() {
    val stream = ByteArrayInputStream(ByteArray(0))
    val result =
      ManifestParsers.parse(
        uri = URI.create("urn:empty"),
        streams = { stream }
      )
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is ParseResult.Failure)
  }

  @Test
  fun testErrorMinimal_0() {
    val result =
      ManifestParsers.parse(
        uri = URI.create("urn:minimal"),
        streams = { resource("error_minimal_0.json") }
      )
    this.log().debug("result: {}", result)
    assertTrue("Result is failure", result is ParseResult.Failure)
  }

  @Test
  fun testOkMinimal_0() {
    val result =
      ManifestParsers.parse(
        uri = URI.create("urn:minimal"),
        streams = { resource("ok_minimal_0.json")}
      )
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    val manifest = success.result

    Assert.assertEquals(3, manifest.readingOrder.size)
    Assert.assertEquals("Track 0", manifest.readingOrder[0].title.toString())
    Assert.assertEquals("100.0", manifest.readingOrder[0].duration.toString())
    Assert.assertEquals("audio/mpeg", manifest.readingOrder[0].type.toString())
    Assert.assertEquals("http://www.example.com/0.mp3", manifest.readingOrder[0].hrefURI.toString())

    Assert.assertEquals("Track 1", manifest.readingOrder[1].title.toString())
    Assert.assertEquals("200.0", manifest.readingOrder[1].duration.toString())
    Assert.assertEquals("audio/mpeg", manifest.readingOrder[1].type.toString())
    Assert.assertEquals("http://www.example.com/1.mp3", manifest.readingOrder[1].hrefURI.toString())

    Assert.assertEquals("Track 2", manifest.readingOrder[2].title.toString())
    Assert.assertEquals("300.0", manifest.readingOrder[2].duration.toString())
    Assert.assertEquals("audio/mpeg", manifest.readingOrder[2].type.toString())
    Assert.assertEquals("http://www.example.com/2.mp3", manifest.readingOrder[2].hrefURI.toString())

    Assert.assertEquals("title", manifest.metadata.title)
    Assert.assertEquals("urn:id", manifest.metadata.identifier)
  }

  @Test
  fun testOkFlatlandGardeur() {
    val result =
      ManifestParsers.parse(
        uri = URI.create("flatland"),
        streams = { resource("flatland.audiobook-manifest.json") }
      )
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    val manifest = success.result

    Assert.assertEquals(
      "Flatland: A Romance of Many Dimensions",
      manifest.metadata.title)
    Assert.assertEquals(
      "https://librivox.org/flatland-a-romance-of-many-dimensions-by-edwin-abbott-abbott/",
      manifest.metadata.identifier)

    Assert.assertEquals(
      9,
      manifest.readingOrder.size)

    Assert.assertEquals(
      "Part 1, Sections 1 - 3",
      manifest.readingOrder[0].title.toString())
    Assert.assertEquals(
      "Part 1, Sections 4 - 5",
      manifest.readingOrder[1].title.toString())
    Assert.assertEquals(
      "Part 1, Sections 6 - 7",
      manifest.readingOrder[2].title.toString())
    Assert.assertEquals(
      "Part 1, Sections 8 - 10",
      manifest.readingOrder[3].title.toString())
    Assert.assertEquals(
      "Part 1, Sections 11 - 12",
      manifest.readingOrder[4].title.toString())
    Assert.assertEquals(
      "Part 2, Sections 13 - 14",
      manifest.readingOrder[5].title.toString())
    Assert.assertEquals(
      "Part 2, Sections 15 - 17",
      manifest.readingOrder[6].title.toString())
    Assert.assertEquals(
      "Part 2, Sections 18 - 20",
      manifest.readingOrder[7].title.toString())
    Assert.assertEquals(
      "Part 2, Sections 21 - 22",
      manifest.readingOrder[8].title.toString())

    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[0].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[1].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[2].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[3].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[4].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[5].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[6].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[7].type.toString())
    Assert.assertEquals(
      "audio/mpeg",
      manifest.readingOrder[8].type.toString())

    Assert.assertEquals(
      "1371.0",
      manifest.readingOrder[0].duration.toString())
    Assert.assertEquals(
      "1669.0",
      manifest.readingOrder[1].duration.toString())
    Assert.assertEquals(
      "1506.0",
      manifest.readingOrder[2].duration.toString())
    Assert.assertEquals(
      "1798.0",
      manifest.readingOrder[3].duration.toString())
    Assert.assertEquals(
      "1225.0",
      manifest.readingOrder[4].duration.toString())
    Assert.assertEquals(
      "1659.0",
      manifest.readingOrder[5].duration.toString())
    Assert.assertEquals(
      "2086.0",
      manifest.readingOrder[6].duration.toString())
    Assert.assertEquals(
      "2662.0",
      manifest.readingOrder[7].duration.toString())
    Assert.assertEquals(
      "1177.0",
      manifest.readingOrder[8].duration.toString())

    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3",
      manifest.readingOrder[0].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3",
      manifest.readingOrder[1].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_3_abbott.mp3",
      manifest.readingOrder[2].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_4_abbott.mp3",
      manifest.readingOrder[3].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_5_abbott.mp3",
      manifest.readingOrder[4].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_6_abbott.mp3",
      manifest.readingOrder[5].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_7_abbott.mp3",
      manifest.readingOrder[6].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_8_abbott.mp3",
      manifest.readingOrder[7].hrefURI.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_9_abbott.mp3",
      manifest.readingOrder[8].hrefURI.toString())
  }

  @Test
  fun testOkFeedbooks_0() {
    val result =
      ManifestParsers.parse(
        uri = URI.create("feedbooks"),
        streams = { resource("feedbooks_0.json") }
      )
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    val manifest = success.result

    Assert.assertEquals(
      "http://archive.org/details/gleams_of_sunshine_1607_librivox",
      manifest.metadata.identifier)
    Assert.assertEquals(
      "Gleams of Sunshine",
      manifest.metadata.title)

    Assert.assertEquals(1, manifest.readingOrder.size)

    run {
      Assert.assertEquals(
        128.0,
        manifest.readingOrder[0].duration)
      Assert.assertEquals(
        "01 - Invocation",
        manifest.readingOrder[0].title)
      Assert.assertEquals(
        120.0,
        manifest.readingOrder[0].bitrate)
      Assert.assertEquals(
        "audio/mpeg",
        manifest.readingOrder[0].type?.fullType)
      Assert.assertEquals(
        "http://archive.org/download/gleams_of_sunshine_1607_librivox/gleamsofsunshine_01_chant.mp3",
        manifest.readingOrder[0].hrefURI.toString())

      val encrypted0 = manifest.readingOrder[0].properties!!.encrypted!!
      Assert.assertEquals("http://www.feedbooks.com/audiobooks/access-restriction", encrypted0.scheme)
      Assert.assertEquals("https://www.cantookaudio.com", (encrypted0.values["profile"] as PlayerManifestScalar.PlayerManifestScalarString).text)
    }

    Assert.assertEquals(3, manifest.links.size)
    Assert.assertEquals(
      "cover",
      manifest.links[0].relation[0])
    Assert.assertEquals(
      180,
      manifest.links[0].width)
    Assert.assertEquals(
      180,
      manifest.links[0].height)
    Assert.assertEquals(
      "image/jpeg",
      manifest.links[0].type?.fullType)
    Assert.assertEquals(
      "http://archive.org/services/img/gleams_of_sunshine_1607_librivox",
      manifest.links[0].hrefURI.toString())

    Assert.assertEquals(
      "self",
      manifest.links[1].relation[0])
    Assert.assertEquals(
      "application/audiobook+json",
      manifest.links[1].type?.fullType)
    Assert.assertEquals(
      "https://api.archivelab.org/books/gleams_of_sunshine_1607_librivox/opds_audio_manifest",
      manifest.links[1].hrefURI.toString())

    Assert.assertEquals(
      "license",
      manifest.links[2].relation[0])
    Assert.assertEquals(
      "application/vnd.readium.license.status.v1.0+json",
      manifest.links[2].type?.fullType)
    Assert.assertEquals(
      "http://example.com/license/status",
      manifest.links[2].hrefURI.toString())
  }

  private fun resource(name: String): InputStream {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return PlayerManifestContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
