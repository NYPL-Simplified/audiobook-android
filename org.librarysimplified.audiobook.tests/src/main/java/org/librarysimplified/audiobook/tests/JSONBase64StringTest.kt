package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.json_web_token.JSONBase64String
import java.security.SecureRandom

class JSONBase64StringTest {

  @Test
  fun testDecodeSomething0()
  {
    val rng = SecureRandom()
    val decoded = ByteArray(32)
    rng.nextBytes(decoded)

    val encoded = JSONBase64String.encode(decoded)
    val decodedAfter = encoded.decode()
    Assert.assertArrayEquals(decodedAfter, decoded)
  }
}
