package org.librarysimplified.audiobook.downloads

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.SettableFuture
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.PlayerDownloadRequestCredentials.Basic
import org.librarysimplified.audiobook.api.PlayerDownloadRequestCredentials.BearerToken
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/*
 * A simple download provider.
 */

class DownloadProvider private constructor(
  private val executor: ListeningExecutorService
) : PlayerDownloadProviderType {

  private val log = LoggerFactory.getLogger(DownloadProvider::class.java)

  companion object {

    /**
     * Create a new download provider.
     *
     * @param executor A listening executor that will be used for download tasks
     */

    fun create(executor: ListeningExecutorService): PlayerDownloadProviderType {
      return DownloadProvider(executor)
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

  private fun reportProgress(
    request: PlayerDownloadRequest,
    percent: Int
  ) {
    try {
      request.onProgress(percent)
    } catch (e: Throwable) {
      this.log.error("ignored onProgress exception: ", e)
    }
  }

  private fun doCleanUp(request: PlayerDownloadRequest) {
    this.log.debug("cleaning up output file {}", request.outputFile)
    DownloadFileIO.fileDelete(request.outputFile)
  }

  private fun doDownload(
    request: PlayerDownloadRequest,
    result: SettableFuture<Unit>
  ) {
    this.log.debug("downloading {} to {}", request.uri, request.outputFile)

    this.reportProgress(request, 0)

    val client =
      OkHttpClient.Builder()
        .connectTimeout(10L, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val httpRequestBuilder =
      Request.Builder()
        .header("User-Agent", request.userAgent.userAgent)
        .url(request.uri.toURL())

    this.configureRequestCredentials(request, httpRequestBuilder)
    val httpRequest = httpRequestBuilder.build()
    val call = client.newCall(httpRequest)
    this.log.debug("executing http request")

    call.execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException(
          StringBuilder(128)
            .append("Server returned an error response.\n")
            .append("  Response: ")
            .append(response.code)
            .append(' ')
            .append(response.message)
            .append('\n')
            .toString()
        )
      }

      this.handleSuccessfulResponse(response, request, result)
    }
  }

  private fun configureRequestCredentials(
    request: PlayerDownloadRequest,
    httpRequestBuilder: Request.Builder
  ) {
    return when (val credentials = request.credentials) {
      null -> {
        this.log.debug("not using authentication")
      }
      is Basic -> {
        this.log.debug("using basic auth")
        httpRequestBuilder.header(
          "Authorization",
          Credentials.basic(credentials.user, credentials.password)
        )
        Unit
      }
      is BearerToken -> {
        this.log.debug("using bearer token auth")
        httpRequestBuilder.header(
          "Authorization",
          "Bearer ${credentials.token}"
        )
        Unit
      }
    }
  }

  private fun handleSuccessfulResponse(
    response: Response,
    request: PlayerDownloadRequest,
    result: SettableFuture<Unit>
  ) {

    /*
     * Check if the future has been cancelled. If it has, don't start copying.
     */

    if (result.isCancelled) {
      this.log.debug("download cancelled")
      throw CancellationException()
    }

    val body =
      response.body ?: throw IOException("HTTP server response did not contain a body")
    val expectedLength =
      body.contentLength()

    /*
     * Try to create the parent directory (and all of the required ancestors too). Ignore
     * errors, because the actual error will occur when an attempt is made to open the file.
     */

    val directory = request.outputFile.parentFile
    directory.mkdirs()

    body.byteStream().use { input_stream ->

      /*
       * XXX: When API level 27 becomes the new minimum API, use the Files class with atomic
       * renames rather than just replacing the output file directly.
       */

      FileOutputStream(request.outputFile, false).use { output_stream ->
        this.copyStream(request, input_stream, output_stream, expectedLength, result)
      }
    }

    val receivedSize = request.outputFile.length()
    if (receivedSize != expectedLength) {
      throw IOException(
        StringBuilder(128)
          .append("Resulting file size does not match the expected size.\n")
          .append("  Expected size: ")
          .append(expectedLength)
          .append('\n')
          .append("  Received size: ")
          .append(receivedSize)
          .append('\n')
          .toString()
      )
    }
  }

  private fun copyStream(
    request: PlayerDownloadRequest,
    inputStream: InputStream,
    outputStream: FileOutputStream,
    expectedLength: Long,
    result: SettableFuture<Unit>
  ) {

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
