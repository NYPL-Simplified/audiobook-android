package org.nypl.audiobook.android.mocking

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementType
import rx.subjects.BehaviorSubject
import java.util.concurrent.ExecutorService

/**
 * A fake spine element in a fake audio book.
 */

class MockingSpineElement(
  val bookMocking: MockingAudioBook,
  val downloadStatusExecutor: ExecutorService,
  val downloadProvider: PlayerDownloadProviderType,
  val downloadStatusEvents: BehaviorSubject<PlayerSpineElementDownloadStatus>,
  override val index: Int,
  override val duration: Duration,
  override val id: String,
  override val title: String) : PlayerSpineElementType {

  var downloadTasksAreSupported = true

  override val downloadTasksSupported: Boolean
    get() = this.downloadTasksAreSupported

  override val book: PlayerAudioBookType
    get() = this.bookMocking

  override val next: PlayerSpineElementType?
    get() =
      if (this.index + 1 < this.bookMocking.spineItems.size) {
        this.bookMocking.spineItems[this.index + 1]
      } else {
        null
      }

  override val previous: PlayerSpineElementType?
    get() =
      if (this.index > 0) {
        this.bookMocking.spineItems[this.index - 1]
      } else {
        null
      }

  override val position: PlayerPosition
    get() = PlayerPosition(title = this.title, offsetMilliseconds = 0, part = 0, chapter = this.index + 1)

  private var downloadStatusValue: PlayerSpineElementDownloadStatus =
    PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded(this)

  fun setDownloadStatus(status: PlayerSpineElementDownloadStatus) {
    this.downloadStatusValue = status
    this.downloadStatusEvents.onNext(status)
  }

  override val downloadStatus: PlayerSpineElementDownloadStatus
    get() = this.downloadStatusValue

  private val downloadTaskValue =
    MockingDownloadTask(
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadProvider = this.downloadProvider,
      spineElement = this)

  override fun downloadTask(): PlayerDownloadTaskType {
    return this.downloadTaskValue
  }
}