package org.librarysimplified.audiobook.tests

import okhttp3.Call
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicParameters
import org.librarysimplified.audiobook.manifest_fulfill.basic.ManifestFulfillmentBasicProvider
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentError
import org.mockito.Mockito
import java.net.URI
import java.net.URL

abstract class ManifestFulfillmentBasicContract {

  private lateinit var call: Call
  private lateinit var client: OkHttpClient

  @Before
  fun testSetup() {
    this.client =
      Mockito.mock(OkHttpClient::class.java)
    this.call =
      Mockito.mock(Call::class.java)
  }

  /**
   * If the server returns a 404 code, the request fails.
   */

  @Test
  fun test404() {
    val response =
      Response.Builder()
        .code(404)
        .message("NOT FOUND")
        .protocol(Protocol.HTTP_1_1)
        .request(
          Request.Builder()
            .url(URL("http://www.example.com"))
            .build()
        )
        .build()

    Mockito.`when`(this.client.newCall(Mockito.any()))
      .thenReturn(this.call)
    Mockito.`when`(this.call.execute())
      .thenReturn(response)

    val provider =
      ManifestFulfillmentBasicProvider(
        client = this.client
      )

    val strategy =
      provider.create(
        configuration = ManifestFulfillmentBasicParameters(
          uri = URI.create("http://www.example.com"),
          userName = "user",
          password = "password"
        )
      )

    val result =
      strategy.execute() as PlayerResult.Failure

    val error =
      result.failure as ManifestFulfillmentError.HTTPRequestFailed
    Assert.assertEquals(404, error.code)
    Assert.assertEquals("NOT FOUND", error.message)
    Assert.assertArrayEquals(ByteArray(0), error.receivedBody)
    Assert.assertEquals("application/octet-stream", error.receivedContentType)
  }

  /**
   * If the server returns a data, the data is returned!
   */

  @Test
  fun testOK() {
    val response =
      Response.Builder()
        .code(200)
        .message("OK")
        .protocol(Protocol.HTTP_1_1)
        .request(
          Request.Builder()
            .url(URL("http://www.example.com"))
            .build()
        )
        .body(
          ResponseBody.create(
            MediaType.parse("text/plain"),
            "Some text."
          )
        )
        .build()

    Mockito.`when`(this.client.newCall(Mockito.any()))
      .thenReturn(this.call)
    Mockito.`when`(this.call.execute())
      .thenReturn(response)

    val provider =
      ManifestFulfillmentBasicProvider(
        client = this.client
      )

    val strategy =
      provider.create(
        configuration = ManifestFulfillmentBasicParameters(
          uri = URI.create("http://www.example.com"),
          userName = "user",
          password = "password"
        )
      )

    val result =
      strategy.execute() as PlayerResult.Success

    val data = result.result
    Assert.assertEquals("Some text.", String(data))
  }
}
