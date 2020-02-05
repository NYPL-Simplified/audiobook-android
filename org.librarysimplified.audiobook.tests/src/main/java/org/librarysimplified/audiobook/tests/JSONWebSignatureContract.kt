package org.librarysimplified.audiobook.tests

import org.junit.Test
import org.librarysimplified.audiobook.json_web_token.JOSEHeader
import org.librarysimplified.audiobook.json_web_token.JSONWebSignature
import org.librarysimplified.audiobook.json_web_token.JSONWebSignatureAlgorithmHMACSha256
import org.librarysimplified.audiobook.json_web_token.JSONWebTokenClaims
import org.quicktheories.QuickTheory
import org.quicktheories.core.Gen
import org.quicktheories.generators.SourceDSL
import java.util.concurrent.TimeUnit

abstract class JSONWebSignatureContract {

  /**
   * Signing produces a verifiable signature.
   */

  @Test
  fun testVerification() {
    val theory =
      QuickTheory.qt()
        .withTestingTime(3L, TimeUnit.SECONDS)
        .withFixedSeed(0xdeadbeefL)

    val mapStringGenerator =
      SourceDSL.strings().basicLatinAlphabet().ofLengthBetween(0, 256)
    val mapGenerator0 =
      SourceDSL.maps().of(mapStringGenerator, mapStringGenerator).ofSizeBetween(0, 256)
    val mapGenerator1 =
      SourceDSL.maps().of(mapStringGenerator, mapStringGenerator).ofSizeBetween(0, 256)
    val mapPairGenerator =
      mapGenerator0.flatMap { map0 ->
        mapGenerator1.flatMap { map1 ->
          Gen {
            Pair(map0, map1)
          }
        }
      }

    val signatureAlgorithm =
      JSONWebSignatureAlgorithmHMACSha256.withSecret("ne cede malis")

    theory.forAll(mapPairGenerator).check { maps ->
      val origHeader =
        JOSEHeader(maps.first)
      val origClaims =
        JSONWebTokenClaims(maps.second)
      val signature =
        JSONWebSignature.create(signatureAlgorithm, origHeader, origClaims)

      signature.verify(signatureAlgorithm)
    }
  }
}
