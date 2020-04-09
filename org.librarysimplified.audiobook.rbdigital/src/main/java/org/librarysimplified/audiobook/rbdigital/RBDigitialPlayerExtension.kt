package org.librarysimplified.audiobook.rbdigital

import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.ExecutorService

class RBDigitialPlayerExtension : PlayerExtensionType {

  private val logger =
    LoggerFactory.getLogger(RBDigitialPlayerExtension::class.java)

  override val name: String =
    "org.librarysimplified.audiobook.rbdigital"

  override fun onDownloadLink(
    statusExecutor: ExecutorService,
    downloadProvider: PlayerDownloadProviderType,
    originalRequest: PlayerDownloadRequest,
    link: PlayerManifestLink
  ): ListenableFuture<Unit>? {
    return if (link.type?.fullType == "vnd.librarysimplified/rbdigital-access-document+json") {
      this.processRBDigitalAccessDocument(
        statusExecutor = statusExecutor,
        downloadProvider = downloadProvider,
        originalRequest = originalRequest,
        link = link
      )
    } else {
      return null
    }
  }

  private fun processRBDigitalAccessDocument(
    statusExecutor: ExecutorService,
    downloadProvider: PlayerDownloadProviderType,
    originalRequest: PlayerDownloadRequest,
    link: PlayerManifestLink
  ): FluentFuture<Unit> {

    if (link !is PlayerManifestLink.LinkBasic) {
      return FluentFuture.from(
        Futures.immediateFailedFuture(
          IllegalArgumentException("Cannot download templated links!")
        )
      )
    }

    val targetURI =
      link.hrefURI ?: return FluentFuture.from(
        Futures.immediateFailedFuture(
          IllegalArgumentException("Target URI is null")
        )
      )

    /*
     * Download the RBDigital link document to a temporary file, and then extract the
     * actual link from that file.
     */

    this.logger.debug("downloading rbdigital link document: {}", targetURI)

    val tempFile =
      File.createTempFile("org.librarysimplified.audiobook.open_access.download-", "tmp")
        .absoluteFile

    this.logger.debug("downloading rbdigital temporary: {}", tempFile)

    val future =
      FluentFuture.from(
        downloadProvider.download(
          PlayerDownloadRequest(
            uri = targetURI,
            credentials = null,
            outputFile = tempFile,
            onProgress = { percent ->
              this.logger.debug("downloading rbdigital link: {}%", percent)
            },
            userAgent = originalRequest.userAgent
          )
        )
      )

    return future.transformAsync(AsyncFunction<Unit, Unit> {
      val uri = this.parseRBDigitalLinkDocument(tempFile)
      tempFile.delete()
      downloadProvider.download(originalRequest.copy(uri = uri))
    }, statusExecutor)
  }

  private fun parseRBDigitalLinkDocument(tempFile: File): URI {
    return when (val result = RBDigitalLinkDocumentParser().parseFromFile(tempFile)) {
      is RBDigitalLinkDocumentParser.ParseResult.ParseSuccess -> result.document.uri
      is RBDigitalLinkDocumentParser.ParseResult.ParseFailed -> throw result.exception
    }
  }
}
