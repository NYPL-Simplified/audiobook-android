package org.librarysimplified.audiobook.open_access

import com.google.common.base.Function
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import net.jcip.annotations.GuardedBy
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.PlayerDownloadTaskType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.open_access.ExoDownloadTask.State.Downloaded
import org.librarysimplified.audiobook.open_access.ExoDownloadTask.State.Downloading
import org.librarysimplified.audiobook.open_access.ExoDownloadTask.State.Initial
import org.librarysimplified.audiobook.rbdigital.RBDigitalLinkDocumentParser
import org.librarysimplified.audiobook.rbdigital.RBDigitalLinkDocumentParser.ParseResult.ParseFailed
import org.librarysimplified.audiobook.rbdigital.RBDigitalLinkDocumentParser.ParseResult.ParseSuccess
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService

/**
 * An Exo implementation of the download task.
 */

class ExoDownloadTask(
  private val downloadStatusExecutor: ExecutorService,
  private val downloadProvider: PlayerDownloadProviderType,
  private val manifest: ExoManifest,
  private val spineElement: ExoSpineElement)
  : PlayerDownloadTaskType {

  private val log = LoggerFactory.getLogger(ExoDownloadTask::class.java)

  private var percent: Int = 0
  private val stateLock: Any = Object()
  @GuardedBy("stateLock")
  private var state: State =
    when (this.spineElement.partFile.isFile) {
      true -> Downloaded
      false -> Initial
    }

  init {
    this.onBroadcastState()
  }

  private sealed class State {
    object Initial : State()
    object Downloaded : State()
    data class Downloading(val future: ListenableFuture<Unit>) : State()
  }

  private fun stateGetCurrent() =
    synchronized(this.stateLock) { this.state }

  private fun stateSetCurrent(new_state: State) =
    synchronized(this.stateLock) { this.state = new_state }

  private fun onBroadcastState() {
    when (this.stateGetCurrent()) {
      Initial -> this.onNotDownloaded()
      Downloaded -> this.onDownloaded()
      is Downloading -> this.onDownloading(this.percent)
    }
  }

  private fun onNotDownloaded() {
    this.log.debug("not downloaded")
    this.spineElement.setDownloadStatus(PlayerSpineElementNotDownloaded(this.spineElement))
  }

  private fun onDownloading(percent: Int) {
    this.percent = percent
    this.spineElement.setDownloadStatus(PlayerSpineElementDownloading(this.spineElement, percent))
  }

  private fun onDownloaded() {
    this.log.debug("downloaded")
    this.spineElement.setDownloadStatus(PlayerSpineElementDownloaded(this.spineElement))
  }

  private fun onStartDownload(): ListenableFuture<Unit> {
    this.log.debug("starting download")

    /*
     * If the spine element requires going through an RBDigital access document, download
     * and parse that first.
     */

    val uriFuture: FluentFuture<URI> =
      when (this.spineElement.itemManifest.type) {
        "vnd.librarysimplified/rbdigital-access-document+json" ->
          this.processRBDigitalAccessDocument(this.spineElement.itemManifest.uri)
        else ->
          FluentFuture.from(Futures.immediateFuture(this.spineElement.itemManifest.uri))
      }

    return uriFuture.transformAsync(
      AsyncFunction<URI, Unit>{ targetURI -> this.onStartDownloadDirect(targetURI!!) },
      MoreExecutors.directExecutor())
  }

  private fun onStartDownloadDirect(targetURI: URI): ListenableFuture<Unit> {
    this.log.debug("download: {}", targetURI)

    val future = this.downloadProvider.download(PlayerDownloadRequest(
      uri = targetURI,
      credentials = null,
      outputFile = this.spineElement.partFile,
      onProgress = { percent -> this.onDownloading(percent) }))

    this.stateSetCurrent(Downloading(future))
    this.onBroadcastState()

    /*
     * Add a callback to the future that will report download success and failure.
     */

    Futures.addCallback(future, object : FutureCallback<Unit> {
      override fun onSuccess(result: Unit?) {
        this@ExoDownloadTask.onDownloadCompleted()
      }

      override fun onFailure(exception: Throwable?) {
        when (exception) {
          is CancellationException ->
            this@ExoDownloadTask.onDownloadCancelled()
          else ->
            this@ExoDownloadTask.onDownloadFailed(Exception(exception))
        }
      }
    }, this.downloadStatusExecutor)

    return future
  }

  private fun processRBDigitalAccessDocument(targetURI: URI): FluentFuture<URI> {
    this.log.debug("downloading rbdigital link document: {}", targetURI)

    val tempFile =
      File.createTempFile("org.librarysimplified.audiobook.open_access.download-", "tmp")
        .absoluteFile

    this.log.debug("downloading rbdigital temporary: {}", tempFile)

    val future =
      FluentFuture.from(
        this.downloadProvider.download(PlayerDownloadRequest(
          uri = targetURI,
          credentials = null,
          outputFile = tempFile,
          onProgress = { percent -> this.log.debug("downloading rbdigital link: {}%", percent) })))

    return future.transform(Function<Unit, URI> {
      val uri = this.parseRBDigitalLinkDocument(tempFile)
      tempFile.delete()
      uri
    }, this.downloadStatusExecutor)
  }

  private fun parseRBDigitalLinkDocument(tempFile: File): URI {
    val result = RBDigitalLinkDocumentParser().parseFromFile(tempFile)
    return when (result) {
      is ParseSuccess -> result.document.uri
      is ParseFailed -> throw result.exception
    }
  }

  private fun onDownloadCancelled() {
    this.log.error("onDownloadCancelled")
    this.stateSetCurrent(Initial)
    this.onDeleteDownloaded()
  }

  private fun onDownloadFailed(e: Exception) {
    this.log.error("onDownloadFailed: ", e)
    this.stateSetCurrent(Initial)
    this.spineElement.setDownloadStatus(
      PlayerSpineElementDownloadFailed(
        this.spineElement, e, e.message ?: "Missing exception message"))
  }

  private fun onDownloadCompleted() {
    this.log.debug("onDownloadCompleted")
    this.stateSetCurrent(Downloaded)
    this.onBroadcastState()
  }

  private fun onDeleteDownloading(state: Downloading) {
    this.log.debug("cancelling download in progress")

    state.future.cancel(true)
    this.stateSetCurrent(Initial)
    this.onDeleteDownloaded()
  }

  private fun onDeleteDownloaded() {
    this.log.debug("deleting file {}", this.spineElement.partFile)

    try {
      ExoFileIO.fileDelete(this.spineElement.partFile)
    } catch (e: Exception) {
      this.log.error("failed to delete file: ", e)
    } finally {
      this.stateSetCurrent(Initial)
      this.onBroadcastState()
    }
  }

  override fun delete() {
    this.log.debug("delete")

    val current = this.stateGetCurrent()
    return when (current) {
      Initial -> this.onBroadcastState()
      Downloaded -> this.onDeleteDownloaded()
      is Downloading -> this.onDeleteDownloading(current)
    }
  }

  override fun fetch() {
    this.log.debug("fetch")

    when (this.stateGetCurrent()) {
      Initial -> this.onStartDownload()
      Downloaded -> this.onDownloaded()
      is Downloading -> this.onDownloading(this.percent)
    }
  }

  override fun cancel() {
    this.log.debug("cancel")

    val current = this.stateGetCurrent()
    return when (current) {
      Initial -> this.onBroadcastState()
      Downloaded -> this.onBroadcastState()
      is Downloading -> this.onDeleteDownloading(current)
    }
  }

  override val progress: Double
    get() = this.percent.toDouble()
}
