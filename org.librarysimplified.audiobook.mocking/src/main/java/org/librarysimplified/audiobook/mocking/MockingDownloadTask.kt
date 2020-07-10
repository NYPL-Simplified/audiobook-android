package org.librarysimplified.audiobook.mocking

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import org.librarysimplified.audiobook.api.PlayerDownloadTaskType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService

/**
 * A fake download task.
 */

class MockingDownloadTask(
  private val downloadStatusExecutor: ExecutorService,
  private val downloadProvider: PlayerDownloadProviderType,
  private val spineElement: MockingSpineElement
) : PlayerDownloadTaskType {

  private val log = LoggerFactory.getLogger(MockingDownloadTask::class.java)

  private var percent: Int = 0
  private val stateLock: Any = Object()
  private var state: State = State.Initial

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
      State.Initial -> this.onNotDownloaded()
      State.Downloaded -> this.onDownloaded()
      is State.Downloading -> this.onDownloading(this.percent)
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

    val future =
      this.downloadProvider.download(
        PlayerDownloadRequest(
          uri = URI.create("urn:" + this.spineElement.index),
          credentials = null,
          outputFile = File("/"),
          userAgent = PlayerUserAgent("org.librarysimplified.audiobook.mocking 1.0.0"),
          onProgress = { percent -> this.onDownloading(percent) }
        )
      )

    this.stateSetCurrent(State.Downloading(future))
    this.onBroadcastState()

    /*
     * Add a callback to the future that will report download success and failure.
     */

    Futures.addCallback(
      future,
      object : FutureCallback<Unit> {
        override fun onSuccess(result: Unit?) {
          this@MockingDownloadTask.onDownloadCompleted()
        }

        override fun onFailure(exception: Throwable?) {
          when (exception) {
            is CancellationException ->
              this@MockingDownloadTask.onDownloadCancelled()
            else ->
              this@MockingDownloadTask.onDownloadFailed(kotlin.Exception(exception))
          }
        }
      },
      this.downloadStatusExecutor
    )

    return future
  }

  private fun onDownloadCancelled() {
    this.log.error("onDownloadCancelled")
    this.stateSetCurrent(State.Initial)
    this.onBroadcastState()
    this.onDeleteDownloaded()
  }

  private fun onDownloadFailed(e: Exception) {
    this.log.error("onDownloadFailed: ", e)
    this.stateSetCurrent(State.Initial)
    this.onBroadcastState()
    this.spineElement.setDownloadStatus(
      PlayerSpineElementDownloadFailed(
        this.spineElement, e, e.message ?: "Missing exception message"
      )
    )
  }

  private fun onDownloadCompleted() {
    this.log.debug("onDownloadCompleted")
    this.stateSetCurrent(State.Downloaded)
    this.onBroadcastState()
  }

  override fun fetch() {
    this.log.debug("fetch")

    when (this.stateGetCurrent()) {
      State.Initial -> this.onStartDownload()
      State.Downloaded -> this.onDownloaded()
      is State.Downloading -> this.onDownloading(this.percent)
    }
  }

  override fun delete() {
    this.log.debug("delete")

    val current = this.stateGetCurrent()
    when (current) {
      State.Initial -> this.onBroadcastState()
      State.Downloaded -> this.onDeleteDownloaded()
      is State.Downloading -> this.onDeleteDownloading(current)
    }
  }

  override fun cancel() {
    this.log.debug("cancel")

    val current = this.stateGetCurrent()
    when (current) {
      State.Initial -> this.onBroadcastState()
      State.Downloaded -> this.onBroadcastState()
      is State.Downloading -> this.onDeleteDownloading(current)
    }
  }

  private fun onDeleteDownloading(state: State.Downloading) {
    this.log.debug("cancelling download in progress")

    state.future.cancel(true)
    this.stateSetCurrent(State.Initial)
    this.onBroadcastState()
    this.onDeleteDownloaded()
  }

  private fun onDeleteDownloaded() {
    this.stateSetCurrent(State.Initial)
    this.onBroadcastState()
  }

  override val progress: Double
    get() = this.percent.toDouble()
}
