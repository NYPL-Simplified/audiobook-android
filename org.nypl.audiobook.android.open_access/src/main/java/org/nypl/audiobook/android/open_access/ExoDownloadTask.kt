package org.nypl.audiobook.android.open_access

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.GuardedBy
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadRequest
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Downloaded
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Downloading
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Initial
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

  private var progressPercent: Int = 0
  private val progressLock: Any = Object()
  @GuardedBy("inProgressLock")
  private var progressState: Progress =
    when (this.spineElement.partFile.isFile) {
      true -> Downloaded
      false -> Initial
    }

  init {
    this.onBroadcastState()
  }

  private sealed class Progress {
    object Initial : Progress()
    object Downloaded : Progress()
    data class Downloading(val future: ListenableFuture<Unit>) : Progress()
  }

  override fun fetch() {
    synchronized(this.progressLock, {
      when (this.progressState) {
        Initial -> onStartDownload()
        Downloaded -> onDownloaded()
        is Downloading -> onDownloading(this.progressPercent)
      }
    })
  }

  private fun onBroadcastState() {
    synchronized(this.progressLock, {
      when (this.progressState) {
        Initial -> onNotDownloaded()
        Downloaded -> onDownloaded()
        is Downloading -> onDownloading(this.progressPercent)
      }
    })
  }

  private fun onNotDownloaded() {
    this.log.debug("not downloaded")
    this.spineElement.setDownloadStatus(
      PlayerSpineElementNotDownloaded(this.spineElement))
  }

  private fun onDownloading(percent: Int) {
    this.progressPercent = percent
    this.spineElement.setDownloadStatus(
      PlayerSpineElementDownloading(this.spineElement, percent))
  }

  private fun onDownloaded() {
    this.log.debug("downloaded")
    this.spineElement.setDownloadStatus(
      PlayerSpineElementDownloaded(this.spineElement))
  }

  private fun onStartDownload(): ListenableFuture<Unit> {
    this.log.debug("starting download")

    val future = this.downloadProvider.download(PlayerDownloadRequest(
      uri = spineElement.itemManifest.uri,
      credentials = null,
      outputFile = spineElement.partFile,
      onProgress = { percent -> this.onDownloading(percent) }))

    this.progressState = Downloading(future)
    this.onBroadcastState()

    /*
     * Add a callback to the future that will report download success and failure.
     */

    Futures.addCallback(future, object : FutureCallback<Unit> {
      override fun onSuccess(result: Unit?) {
        this@ExoDownloadTask.onDownloadCompleted()
      }

      override fun onFailure(t: Throwable?) {
        this@ExoDownloadTask.onDownloadFailed(kotlin.Exception(t))
      }
    }, this.downloadStatusExecutor)

    return future
  }

  private fun onDownloadFailed(e: Exception) {
    this.log.error("onDownloadFailed: ", e)

    synchronized(this.progressLock, {
      this.progressState = Initial
      this.spineElement.setDownloadStatus(
        PlayerSpineElementDownloadFailed(
          this.spineElement, e, e.message ?: "Missing exception message"))
    })
  }

  private fun onDownloadCompleted() {
    this.log.debug("onDownloadCompleted")

    synchronized(this.progressLock, {
      this.progressState = Downloaded
      this.onBroadcastState()
    })
  }

  override fun delete() {
    synchronized(this.progressLock, {
      val state = this.progressState
      when (state) {
        Initial -> Unit
        Downloaded -> onDeleteDownloaded()
        is Downloading -> onDeleteDownloading(state)
      }
    })
  }

  private fun onDeleteDownloading(state: Downloading) {
    this.log.debug("cancelling download in progress")

    state.future.cancel(true)
    this.progressState = Initial
    this.onDeleteDownloaded()
  }

  private fun onDeleteDownloaded() {
    this.log.debug("deleting file {}", this.spineElement.partFile)

    try {
      ExoFileIO.fileDelete(this.spineElement.partFile)
    } catch (e: Exception) {
      this.log.error("failed to delete file: ", e)
    } finally {
      this.progressState = Initial
      this.onBroadcastState()
    }
  }

  override val progress: Double
    get() = this.progressPercent.toDouble()
}
