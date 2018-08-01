package org.nypl.audiobook.android.open_access

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.GuardedBy
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadProviderType.Result
import org.nypl.audiobook.android.api.PlayerDownloadRequest
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.open_access.ExoDownloadTask.State.Downloaded
import org.nypl.audiobook.android.open_access.ExoDownloadTask.State.Downloading
import org.nypl.audiobook.android.open_access.ExoDownloadTask.State.Initial
import org.slf4j.LoggerFactory
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
    data class Downloading(val future: ListenableFuture<PlayerDownloadProviderType.Result>) : State()
  }

  override fun fetch() {
    this.log.debug("fetch")

    when (this.stateGetCurrent()) {
      Initial -> onStartDownload()
      Downloaded -> onDownloaded()
      is Downloading -> onDownloading(this.percent)
    }
  }

  private fun stateGetCurrent() =
    synchronized(stateLock) { state }

  private fun stateSetCurrent(new_state: State) =
    synchronized(stateLock) { this.state = new_state }

  private fun onBroadcastState() {
    when (this.stateGetCurrent()) {
      Initial -> onNotDownloaded()
      Downloaded -> onDownloaded()
      is Downloading -> onDownloading(this.percent)
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

  private fun onStartDownload(): ListenableFuture<PlayerDownloadProviderType.Result> {
    this.log.debug("starting download")

    val future = this.downloadProvider.download(PlayerDownloadRequest(
      uri = spineElement.itemManifest.uri,
      credentials = null,
      outputFile = spineElement.partFile,
      onProgress = { percent -> this.onDownloading(percent) }))

    this.stateSetCurrent(Downloading(future))
    this.onBroadcastState()

    /*
     * Add a callback to the future that will report download success and failure.
     */

    Futures.addCallback(future, object : FutureCallback<PlayerDownloadProviderType.Result> {
      override fun onSuccess(result: PlayerDownloadProviderType.Result?) {
        when (result) {
          Result.CANCELLED ->
            this@ExoDownloadTask.onDownloadCancelled()
          Result.SUCCEEDED ->
            this@ExoDownloadTask.onDownloadCompleted()
        }
      }

      override fun onFailure(t: Throwable?) {
        this@ExoDownloadTask.onDownloadFailed(kotlin.Exception(t))
      }
    }, this.downloadStatusExecutor)

    return future
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

  override fun delete() {
    this.log.debug("delete")

    val current = stateGetCurrent()
    when (current) {
      Initial -> Unit
      Downloaded -> onDeleteDownloaded()
      is Downloading -> onDeleteDownloading(current)
    }
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

  override val progress: Double
    get() = this.percent.toDouble()
}
