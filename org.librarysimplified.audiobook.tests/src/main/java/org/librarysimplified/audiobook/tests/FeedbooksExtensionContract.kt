package org.librarysimplified.audiobook.tests

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.PlayerDownloadRequestCredentials
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtension
import org.librarysimplified.audiobook.feedbooks.FeedbooksPlayerExtensionConfiguration
import org.librarysimplified.audiobook.manifest.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLinkProperties
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

abstract class FeedbooksExtensionContract {

  private val logger = LoggerFactory.getLogger(FeedbooksExtensionContract::class.java)
  private lateinit var downloadProvider: FakeDownloadProvider
  private lateinit var executor: ListeningExecutorService

  @Before
  fun testSetup() {
    this.executor =
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    this.downloadProvider =
      FakeDownloadProvider()
  }

  class FakeDownloadProvider : PlayerDownloadProviderType {
    val requests =
      LinkedList<PlayerDownloadRequest>()

    override fun download(
      request: PlayerDownloadRequest
    ): ListenableFuture<Unit> {
      this.requests.add(request)
      return Futures.immediateFuture(Unit)
    }
  }

  @After
  fun testTearDown() {
    this.executor.shutdown()
  }

  /**
   * A link that doesn't contain a protection scheme isn't handled by the extension.
   */

  @Test
  fun testNotApplicable() {
    val extension = FeedbooksPlayerExtension()
    val file = File(TestDirectories.temporaryDirectory(), UUID.randomUUID().toString())

    val request =
      PlayerDownloadRequest(
        uri = URI.create("urn:fake"),
        credentials = null,
        outputFile = file,
        onProgress = {
          this.logger.debug("progress: {}", it)
        },
        userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0")
      )

    val future =
      extension.onDownloadLink(
        statusExecutor = this.executor,
        downloadProvider = this.downloadProvider,
        originalRequest = request,
        link = PlayerManifestLink.LinkBasic(
          href = URI.create("urn:fake")
        )
      )

    Assert.assertEquals(null, future)
  }

  /**
   * A link that contains a Feedbooks protection scheme requires configuration values to be
   * set.
   */

  @Test
  fun testNotConfigured() {
    val extension = FeedbooksPlayerExtension()
    val file = File(TestDirectories.temporaryDirectory(), UUID.randomUUID().toString())

    val request =
      PlayerDownloadRequest(
        uri = URI.create("urn:fake"),
        credentials = null,
        outputFile = file,
        onProgress = {
          this.logger.debug("progress: {}", it)
        },
        userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0")
      )

    val future =
      extension.onDownloadLink(
        statusExecutor = this.executor,
        downloadProvider = this.downloadProvider,
        originalRequest = request,
        link = PlayerManifestLink.LinkBasic(
          href = URI.create("urn:fake"),
          properties = PlayerManifestLinkProperties(
            encrypted = PlayerManifestEncrypted(
              scheme = "http://www.feedbooks.com/audiobooks/access-restriction"
            )
          )
        )
      )

    try {
      future!!.get()
    } catch (e: ExecutionException) {
      val cause = e.cause as IllegalStateException
      Assert.assertTrue(cause.message!!.contains("has not been configured"))
      return
    }

    Assert.fail()
  }

  /**
   * A link that contains a Feedbooks protection scheme requires sends a bearer token.
   */

  @Test
  fun testBearerTokenSent() {
    val extension = FeedbooksPlayerExtension()
    val file = File(TestDirectories.temporaryDirectory(), UUID.randomUUID().toString())

    extension.configuration =
      FeedbooksPlayerExtensionConfiguration(
        bearerTokenSecret = "confound delivery".toByteArray(),
        issuerURL = "http://www.example.com"
      )

    val request =
      PlayerDownloadRequest(
        uri = URI.create("urn:fake"),
        credentials = null,
        outputFile = file,
        onProgress = {
          this.logger.debug("progress: {}", it)
        },
        userAgent = PlayerUserAgent("org.librarysimplified.audiobook.tests 1.0.0")
      )

    val future =
      extension.onDownloadLink(
        statusExecutor = this.executor,
        downloadProvider = this.downloadProvider,
        originalRequest = request,
        link = PlayerManifestLink.LinkBasic(
          href = URI.create("urn:fake"),
          properties = PlayerManifestLinkProperties(
            encrypted = PlayerManifestEncrypted(
              scheme = "http://www.feedbooks.com/audiobooks/access-restriction"
            )
          )
        )
      )

    future!!.get()

    val sentRequest = this.downloadProvider.requests.poll()
    Assert.assertEquals(request.uri, sentRequest.uri)
    Assert.assertEquals(request.outputFile, sentRequest.outputFile)
    Assert.assertTrue(sentRequest.credentials is PlayerDownloadRequestCredentials.BearerToken)
  }
}
