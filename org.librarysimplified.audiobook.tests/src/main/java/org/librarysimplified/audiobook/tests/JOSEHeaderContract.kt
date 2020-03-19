package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.json_web_token.JOSEHeader
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.quicktheories.QuickTheory
import org.quicktheories.generators.SourceDSL
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class JOSEHeaderContract {

  /**
   * Empty JOSE headers are empty.
   */

  @Test
  fun testEmpty() {
    val result =
      JOSEHeader.parse(
        uri = URI("urn:test"),
        data = """{ }""".toByteArray()
      ) as ParseResult.Success

    val token = result.result
    Assert.assertEquals(0, token.headers.size)
  }

  /**
   * Headers can be read.
   */

  @Test
  fun testExample0() {
    val result =
      JOSEHeader.parse(
        uri = URI("urn:test"),
        data = """
     {"typ":"JWT",
      "alg":"HS256"}
          """.trimIndent().toByteArray()
      ) as ParseResult.Success

    val token = result.result
    Assert.assertEquals(2, token.headers.size)
    Assert.assertEquals("JWT", token.headers["typ"])
    Assert.assertEquals("HS256", token.headers["alg"])
  }

  /**
   * Encoding and decoding are inverses of each other.
   */

  @Test(timeout = 10_000L)
  fun testIdentity() {
    val theory =
      QuickTheory.qt()
        .withTestingTime(3L, TimeUnit.SECONDS)
        .withFixedSeed(0xdeadbeefL)

    val mapStringGenerator =
      SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 256)
    val mapGenerator =
      SourceDSL.maps().of(mapStringGenerator, mapStringGenerator).ofSizeBetween(0, 256)

    theory.forAll(mapGenerator).check { map ->
      val originalHeader = JOSEHeader(map.toMap())
      val encoded = JOSEHeader.encode(originalHeader)
      val decodeResult = JOSEHeader.decode(URI.create("urn:test"), encoded)
      require(decodeResult is ParseResult.Success)
      val decodedHeader = decodeResult.result
      originalHeader == decodedHeader
    }
  }
}
