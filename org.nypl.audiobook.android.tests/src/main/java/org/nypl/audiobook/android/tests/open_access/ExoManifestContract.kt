package org.nypl.audiobook.android.tests.open_access

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.nypl.audiobook.android.api.PlayerManifest
import org.nypl.audiobook.android.api.PlayerManifests
import org.nypl.audiobook.android.api.PlayerResult
import org.nypl.audiobook.android.open_access.ExoManifest
import org.slf4j.Logger
import java.io.InputStream

/**
 * Tests for the {@link org.nypl.audiobook.android.api.PlayerRawManifest} type.
 */

abstract class ExoManifestContract {

  abstract fun log(): Logger

  @Test
  fun testOkFlatlandGardeur() {
    val result = PlayerManifests.parse(resource("flatland.audiobook-manifest.json"))
    this.log().debug("result: {}", result)
    assertTrue("Result is success", result is PlayerResult.Success)

    val success: PlayerResult.Success<PlayerManifest, Exception> =
      result as PlayerResult.Success<PlayerManifest, Exception>

    val manifest = success.result

    val exo_result = ExoManifest.transform(manifest)
    this.log().debug("exo_result: {}", exo_result)
    assertTrue("Result is success", exo_result is PlayerResult.Success)

    val exo_success: PlayerResult.Success<ExoManifest, Exception> =
      exo_result as PlayerResult.Success<ExoManifest, Exception>

    val exo = exo_success.result

    Assert.assertEquals(
      "Flatland: A Romance of Many Dimensions",
      exo.title)
    Assert.assertEquals(
      "https://librivox.org/flatland-a-romance-of-many-dimensions-by-edwin-abbott-abbott/",
      exo.id)

    Assert.assertEquals(
      9,
      exo.spineItems.size)

    Assert.assertEquals(
      "Part 1, Sections 1 - 3",
      exo.spineItems[0].title)
    Assert.assertEquals(
      "Part 1, Sections 4 - 5",
      exo.spineItems[1].title)
    Assert.assertEquals(
      "Part 1, Sections 6 - 7",
      exo.spineItems[2].title)
    Assert.assertEquals(
      "Part 1, Sections 8 - 10",
      exo.spineItems[3].title)
    Assert.assertEquals(
      "Part 1, Sections 11 - 12",
      exo.spineItems[4].title)
    Assert.assertEquals(
      "Part 2, Sections 13 - 14",
      exo.spineItems[5].title)
    Assert.assertEquals(
      "Part 2, Sections 15 - 17",
      exo.spineItems[6].title)
    Assert.assertEquals(
      "Part 2, Sections 18 - 20",
      exo.spineItems[7].title)
    Assert.assertEquals(
      "Part 2, Sections 21 - 22",
      exo.spineItems[8].title)

    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[0].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[1].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[2].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[3].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[4].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[5].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[6].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[7].type)
    Assert.assertEquals(
      "audio/mpeg",
      exo.spineItems[8].type)

    Assert.assertEquals(
      "1371.0",
      exo.spineItems[0].duration.toString())
    Assert.assertEquals(
      "1669.0",
      exo.spineItems[1].duration.toString())
    Assert.assertEquals(
      "1506.0",
      exo.spineItems[2].duration.toString())
    Assert.assertEquals(
      "1798.0",
      exo.spineItems[3].duration.toString())
    Assert.assertEquals(
      "1225.0",
      exo.spineItems[4].duration.toString())
    Assert.assertEquals(
      "1659.0",
      exo.spineItems[5].duration.toString())
    Assert.assertEquals(
      "2086.0",
      exo.spineItems[6].duration.toString())
    Assert.assertEquals(
      "2662.0",
      exo.spineItems[7].duration.toString())
    Assert.assertEquals(
      "1177.0",
      exo.spineItems[8].duration.toString())

    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3",
      exo.spineItems[0].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3",
      exo.spineItems[1].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_3_abbott.mp3",
      exo.spineItems[2].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_4_abbott.mp3",
      exo.spineItems[3].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_5_abbott.mp3",
      exo.spineItems[4].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_6_abbott.mp3",
      exo.spineItems[5].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_7_abbott.mp3",
      exo.spineItems[6].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_8_abbott.mp3",
      exo.spineItems[7].uri.toString())
    Assert.assertEquals(
      "http://www.archive.org/download/flatland_rg_librivox/flatland_9_abbott.mp3",
      exo.spineItems[8].uri.toString())

    Assert.assertEquals(
      "0",
      exo.spineItems[0].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[1].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[2].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[3].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[4].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[5].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[6].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[7].part.toString())
    Assert.assertEquals(
      "0",
      exo.spineItems[8].part.toString())

    Assert.assertEquals(
      "0",
      exo.spineItems[0].chapter.toString())
    Assert.assertEquals(
      "1",
      exo.spineItems[1].chapter.toString())
    Assert.assertEquals(
      "2",
      exo.spineItems[2].chapter.toString())
    Assert.assertEquals(
      "3",
      exo.spineItems[3].chapter.toString())
    Assert.assertEquals(
      "4",
      exo.spineItems[4].chapter.toString())
    Assert.assertEquals(
      "5",
      exo.spineItems[5].chapter.toString())
    Assert.assertEquals(
      "6",
      exo.spineItems[6].chapter.toString())
    Assert.assertEquals(
      "7",
      exo.spineItems[7].chapter.toString())
    Assert.assertEquals(
      "8",
      exo.spineItems[8].chapter.toString())
  }

  private fun resource(name: String): InputStream {
    val path = "/org/nypl/audiobook/android/tests/" + name
    return ExoManifestContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
