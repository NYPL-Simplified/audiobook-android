package org.nypl.audiobook.android.open_access

import com.google.common.util.concurrent.ListenableFuture
import net.jcip.annotations.GuardedBy
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadRequest
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Downloaded
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Downloading
import org.nypl.audiobook.android.open_access.ExoDownloadTask.Progress.Initial
import org.slf4j.LoggerFactory
import rx.subjects.PublishSubject


class ExoDownloadTask(
  private val downloadStatusEvents: PublishSubject<PlayerSpineElementDownloadStatus>,
  private val downloadProvider: PlayerDownloadProviderType,
  private val manifest: ExoManifest,
  private val spineElement: ExoSpineElement) : PlayerDownloadTaskType {

  private val log = LoggerFactory.getLogger(ExoDownloadTask::class.java)

  private var progressPercent: Int = 0
  private val progressLock: Any = Object()
  @GuardedBy("inProgressLock")
  private var progressState: Progress =
    when (this.spineElement.partFile.isFile) {
      true -> Downloaded
      false -> Initial
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

  private fun onDownloading(percent: Int) {
    this.progressPercent = percent
    this.downloadStatusEvents.onNext(PlayerSpineElementDownloading(percent))
  }

  private fun onDownloaded() {
    this.downloadStatusEvents.onNext(PlayerSpineElementDownloaded)
  }

  private fun onStartDownload(): ListenableFuture<Unit> {
    this.log.debug("starting download")

    val future = this.downloadProvider.download(PlayerDownloadRequest(
      uri = spineElement.itemManifest.uri,
      credentials = null,
      outputFile = spineElement.partFile,
      onProgress = { percent -> this.onDownloading(percent) }))

    this.progressState = Downloading(future)
    return future
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
    this.log.debug("deleting file ")
    ExoFileIO.fileDelete(this.spineElement.partFile)
  }

  override val progress: Double
    get() = this.progressPercent.toDouble()

  override val id: String
    get() = TODO("not implemented")
}
