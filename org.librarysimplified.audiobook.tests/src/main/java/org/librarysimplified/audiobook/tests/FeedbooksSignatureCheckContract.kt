package org.librarysimplified.audiobook.tests

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.feedbooks.FeedbooksSignatureCheck
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.Logger
import java.io.File
import java.net.URI
import java.util.ServiceLoader

abstract class FeedbooksSignatureCheckContract {

  private lateinit var eventLog: MutableList<SingleLicenseCheckStatus>

  abstract fun log(): Logger

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Before
  fun testSetup() {
    this.eventLog = mutableListOf()
  }

  /**
   * A check with a correct certificate should result in success.
   */
  @Test
  fun verificationSuccess() {
    val manifestName = "feedbooks_2.json"
    val certificateName = "jwks_0.json"

    val httpClient = mockHttpClient(
      respondToUrl = "https://listen.cantookaudio.com/.well-known/jwks.json",
      responseResourceName = certificateName
    )

    val cacheDirectory = emptyCacheDirectory()

    val result =
      FeedbooksSignatureCheck(
        httpClient = httpClient,
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = cacheDirectory
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Succeeded)
  }

  /**
   * A check with an incorrect certificate should result in failure.
   */
  @Test
  fun verificationFailure() {
    val manifestName = "feedbooks_0.json"
    val certificateName = "jwks_0.json"

    val result =
      FeedbooksSignatureCheck(
        httpClient = mockHttpClient(
          respondToUrl = "https://listen.cantookaudio.com/.well-known/jwks.json",
          responseResourceName = certificateName
        ),
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = emptyCacheDirectory()
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Failed)
    Assert.assertTrue(result.message.contains("not verified", true))
  }

  /**
   * A signature with an unsupported algorithm should result in failure.
   */
  @Test
  fun unsupportedAlgorithm() {
    val manifestName = "feedbooks_3.json"
    val certificateName = "jwks_0.json"

    val result =
      FeedbooksSignatureCheck(
        httpClient = mockHttpClient(
          respondToUrl = "https://listen.cantookaudio.com/.well-known/jwks.json",
          responseResourceName = certificateName
        ),
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = emptyCacheDirectory()
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Failed)
    Assert.assertTrue(result.message.contains("unsupported signature algorithm", true))
  }

  /**
   * A signature with an unkonwn issuer should result in failure.
   */
  @Test
  fun unknownIssuer() {
    val manifestName = "feedbooks_1.json"
    val certificateName = "jwks_0.json"

    val result =
      FeedbooksSignatureCheck(
        httpClient = mockHttpClient(
          respondToUrl = "https://listen.cantookaudio.com/.well-known/jwks.json",
          responseResourceName = certificateName
        ),
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = emptyCacheDirectory()
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Failed)
    Assert.assertTrue(result.message.contains("unknown signature issuer", true))
  }

  /**
   * An error retrieving the certificate should result in failure.
   */
  @Test
  fun certificateRetrievalFailure() {
    val manifestName = "feedbooks_0.json"

    val result =
      FeedbooksSignatureCheck(
        httpClient = mockFailingHttpClient(),
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = emptyCacheDirectory()
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Failed)
    Assert.assertTrue(result.message.contains("could not be retrieved", true))
  }

  /**
   * An error parsing the certificate should result in failure.
   */
  @Test
  fun unparseableCertificate() {
    val manifestName = "feedbooks_2.json"
    val certificateName = "jwks_2.json"

    val httpClient = mockHttpClient(
      respondToUrl = "https://listen.cantookaudio.com/.well-known/jwks.json",
      responseResourceName = certificateName
    )

    val cacheDirectory = emptyCacheDirectory()

    val result =
      FeedbooksSignatureCheck(
        httpClient = httpClient,
        parameters = SingleLicenseCheckParameters(
          manifest = this.manifest(manifestName),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0"),
          onStatusChanged = { },
          cacheDirectory = cacheDirectory
        )
      ).execute()

    Assert.assertTrue(result is SingleLicenseCheckResult.Failed)
    Assert.assertTrue(result.message.contains("could not be parsed", true))
  }

  private fun manifest(
    name: String
  ): PlayerManifest {
    val result =
      ManifestParsers.parse(
        uri = URI.create(name),
        streams = this.resource(name),
        extensions = ServiceLoader.load(ManifestParserExtensionType::class.java).toList()
      )
    this.log().debug("result: {}", result)
    Assert.assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    return success.result
  }

  private fun resource(
    name: String
  ): ByteArray {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return FeedbooksSignatureCheckContract::class.java.getResourceAsStream(path)?.readBytes()
      ?: throw AssertionError("Missing resource file: " + path)
  }

  private fun mockHttpClient(respondToUrl: String, responseResourceName: String): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
          val uri = chain.request().url.toUri().toString()

          if (uri == respondToUrl) {
            return chain.proceed(chain.request())
              .newBuilder()
              .code(200)
              .protocol(Protocol.HTTP_1_1)
              .message("OK")
              .body(
                ResponseBody.create(
                  "application/json".toMediaTypeOrNull(),
                  resource(responseResourceName)
                )
              ).addHeader("content-type", "application/json")
              .build()
          }

          return chain.proceed(chain.request())
            .newBuilder()
            .code(404)
            .protocol(Protocol.HTTP_1_1)
            .message("Not Found")
            .body(
              ResponseBody.create(
                "text/plain".toMediaTypeOrNull(),
                "Not found"
              )
            ).addHeader("content-type", "text/plain")
            .build()
        }
      })
      .build()
  }

  private fun mockFailingHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
          return chain.proceed(chain.request())
            .newBuilder()
            .code(500)
            .protocol(Protocol.HTTP_1_1)
            .message("Nope")
            .body(
              ResponseBody.create(
                "text/plain".toMediaTypeOrNull(),
                "Nope nope nope"
              )
            ).addHeader("content-type", "text/plain")
            .build()
        }
      })
      .build()
  }

  private fun emptyCacheDirectory(): File {
    return tempFolder.newFolder("cache")
  }
}
