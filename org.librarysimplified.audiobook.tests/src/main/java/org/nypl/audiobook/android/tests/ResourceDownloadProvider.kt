package org.nypl.audiobook.android.tests

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CancellationException

/*
 * A download provider that loads files from resources.
 */

class ResourceDownloadProvider private constructor(
  private val executor: ListeningExecutorService,
  private val resources: Map<URI, () -> InputStream>)
  : PlayerDownloadProviderType {

  private val log = LoggerFactory.getLogger(ResourceDownloadProvider::class.java)

  companion object {

    /**
     * Create a new download provider.
     *
     * @param executor A listening executor that will be used for download tasks
     */

    fun create(executor: ListeningExecutorService,
               resources: Map<URI, () -> InputStream>): PlayerDownloadProviderType {
      return ResourceDownloadProvider(executor, resources)
    }
  }

  override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
    val result = SettableFuture.create<Unit>()

    this.reportProgress(request, 0)

    this.executor.submit {
      try {
        doDownload(request, result)
        result.set(Unit)
      } catch (e: CancellationException) {
        doCleanUp(request)
      } catch (e: Throwable) {
        result.setException(e)
        doCleanUp(request)
      }
    }
    return result
  }

  private fun reportProgress(request: PlayerDownloadRequest, percent: Int) {
    try {
      request.onProgress(percent)
    } catch (e: Throwable) {
      this.log.error("ignored onProgress exception: ", e)
    }
  }

  private fun doCleanUp(request: PlayerDownloadRequest) {
    this.log.debug("cleaning up output file {}", request.outputFile)
    ResourceFileIO.fileDelete(request.outputFile)
  }

  private fun doDownload(
    request: PlayerDownloadRequest,
    result: SettableFuture<Unit>) {
    this.log.debug("downloading {} to {}", request.uri, request.outputFile)

    this.reportProgress(request, 0)

    if (!this.resources.containsKey(request.uri)) {
      throw IOException("No such URI in resources: " + request.uri)
    }

    /*
      * Check if the future has been cancelled. If it has, don't start copying.
      */

    if (result.isCancelled) {
      this.log.debug("download cancelled")
      throw CancellationException()
    }

    /*
     * Try to create the parent directory (and all of the required ancestors too). Ignore
     * errors, because the actual error will occur when an attempt is made to open the file.
     */

    val directory = request.outputFile.parentFile
    directory.mkdirs()

    this.resources[request.uri]!!().use { input_stream ->

      /*
       * XXX: When API level 27 becomes the new minimum API, use the Files class with atomic
       * renames rather than just replacing the output file directly.
       */

      FileOutputStream(request.outputFile, false).use { output_stream ->
        this.copyStream(request, input_stream, output_stream, input_stream.available().toLong(), result)
      }
    }
  }

  private fun copyStream(
    request: PlayerDownloadRequest,
    inputStream: InputStream,
    outputStream: FileOutputStream,
    expectedLength: Long,
    result: SettableFuture<Unit>) {

    var progressPrevious = 0.0
    var progressCurrent = 0.0
    var received = 0L
    val buffer = ByteArray(1024)

    while (true) {

      /*
       * Check if the future has been cancelled. If it has, stop copying.
       */

      if (result.isCancelled) {
        this.log.debug("download cancelled")
        throw CancellationException()
      }

      val r = inputStream.read(buffer)
      if (r == -1) {
        break
      }
      received += r
      outputStream.write(buffer, 0, r)

      /*
       * Only report progress when the progress has changed by 5% or more. This essentially
       * throttles event delivery to avoid updating progress indicators hundreds of times per
       * second.
       */

      progressCurrent = (received.toDouble() / expectedLength.toDouble()) * 100.0
      if (progressCurrent - progressPrevious > 5.0 || progressCurrent >= 100.0) {
        progressPrevious = progressCurrent
        this.log.debug("download progress: {}", progressCurrent)
      }

      this.reportProgress(request, progressCurrent.toInt())
    }
    outputStream.flush()
  }

}