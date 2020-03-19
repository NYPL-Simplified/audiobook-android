package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Test
import org.librarysimplified.audiobook.json_web_token.JSONBase64String

class JSONBase64StringTest {

  @Test
  fun testDecodeSomething0()
  {
    val base =
      JSONBase64String("HPvmn7jSSUPiDrjWyJOdHh55kQe10R7RcICI1jr39Dw=")
    val decoded =
      base.decode()
    val encoded =
      JSONBase64String.encode(decoded)

    Assert.assertEquals(base, encoded)
  }
}
