package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.json_web_token.JSONWebTokenClaims
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.quicktheories.QuickTheory
import org.quicktheories.generators.SourceDSL
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class JSONWebTokenClaimsContract {

  /**
   * Empty claims are empty.
   */

  @Test
  fun testEmpty() {
    val result =
      JSONWebTokenClaims.parse(
        uri = URI("urn:test"),
        data = """{ }""".toByteArray()
      ) as ParseResult.Success

    val token = result.result
    Assert.assertEquals(0, token.claims.size)
  }

  /**
   * Claims can be retrieved.
   */

  @Test
  fun testExample0() {
    val result =
      JSONWebTokenClaims.parse(
        uri = URI("urn:test"),
        data = """
     {"typ":"JWT",
      "alg":"HS256"}
          """.trimIndent().toByteArray()
      ) as ParseResult.Success

    val token = result.result
    Assert.assertEquals(2, token.claims.size)
    Assert.assertEquals("JWT", token.claims["typ"])
    Assert.assertEquals("HS256", token.claims["alg"])
  }

  /**
   * Encoding and decoding are inverses of each other.
   */

  @Test
  fun testIdentity() {
    val theory =
      QuickTheory.qt()
        .withTestingTime(3L, TimeUnit.SECONDS)
        .withFixedSeed(0xdeadbeefL)

    val mapStringGenerator =
      SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(1, 256)
    val mapGenerator =
      SourceDSL.maps().of(mapStringGenerator, mapStringGenerator).ofSizeBetween(1, 256)

    theory.forAll(mapGenerator).check { map ->
      val originalHeader = JSONWebTokenClaims(map.toMap())
      val encoded = JSONWebTokenClaims.encode(originalHeader)
      val decodeResult = JSONWebTokenClaims.decode(URI.create("urn:test"), encoded)
      require(decodeResult is ParseResult.Success)
      val decodedHeader = decodeResult.result
      originalHeader == decodedHeader
    }
  }
}
