package org.librarysimplified.audiobook.mocking

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * A fake download provider that takes URIs of the form "urn:n" where "n" is a positive integer,
 * and appears to download for "n" seconds.
 */

class MockingDownloadProvider(
  private val shouldFail: (PlayerDownloadRequest) -> Boolean,
  private val executorService: ListeningExecutorService
) : PlayerDownloadProviderType {

  override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
    val result = SettableFuture.create<Unit>()

    this.reportProgress(request, 0)

    this.executorService.submit {
      try {
        if (this.shouldFail.invoke(request)) {
          throw IOException("Failed!")
        }

        doDownload(request, result)
        result.set(Unit)
      } catch (e: CancellationException) {
      } catch (e: Throwable) {
        result.setException(e)
      }
    }
    return result
  }

  private fun reportProgress(request: PlayerDownloadRequest, percent: Int) {
    try {
      request.onProgress(percent)
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  private fun doDownload(request: PlayerDownloadRequest, result: SettableFuture<Unit>) {
    val time = Math.max(1, request.uri.rawSchemeSpecificPart.toInt()) * 10

    request.onProgress(0)
    for (i in 0..time) {
      if (result.isCancelled) {
        throw CancellationException()
      }

      val percent = ((i.toDouble() / time.toDouble()) * 100.0)
      this.reportProgress(request, percent.toInt())
      Thread.sleep(100L)
    }
    return
  }
}
