package org.nypl.audiobook.android.open_access

import net.jcip.annotations.GuardedBy
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementType
import rx.subjects.PublishSubject
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * A spine element in an audio book.
 */

class ExoSpineElement(
  private val downloadStatusEvents: PublishSubject<PlayerSpineElementDownloadStatus>,
  val bookID: PlayerBookID,
  val bookManifest: ExoManifest,
  val itemManifest: ExoManifestSpineItem,
  val partFile: File,
  private val downloadProvider: PlayerDownloadProviderType,
  override val index: Int,
  internal var nextElement: PlayerSpineElementType?,
  internal var previousElement: PlayerSpineElementType?,
  override val duration: Duration,
  val engineExecutor: ExecutorService)
  : PlayerSpineElementType {

  /**
   * The current download status of the spine element.
   */

  private val statusLock: Any = Object()
  @GuardedBy("statusLock")
  private var statusNow: PlayerSpineElementDownloadStatus =
    PlayerSpineElementNotDownloaded(this)

  private lateinit var bookActual: ExoAudioBook

  override val book: PlayerAudioBookType
    get() = this.bookActual

  override val next: PlayerSpineElementType?
    get() = this.nextElement

  override val previous: PlayerSpineElementType?
    get() = this.previousElement

  override val position: PlayerPosition
    get() = PlayerPosition(
      this.itemManifest.title,
      this.itemManifest.part,
      this.itemManifest.chapter,
      0)

  override val title: String
    get() = this.itemManifest.title

  private val downloadTask: PlayerDownloadTaskType =
    ExoDownloadTask(
      downloadStatusExecutor = this.engineExecutor,
      downloadProvider = this.downloadProvider,
      manifest = this.bookManifest,
      spineElement = this)

  override fun downloadTask(): PlayerDownloadTaskType {
    return this.downloadTask
  }

  fun setBook(book: ExoAudioBook) {
    this.bookActual = book
  }

  fun setDownloadStatus(status: PlayerSpineElementDownloadStatus) {
    synchronized(this.statusLock, { this.statusNow = status })
    this.downloadStatusEvents.onNext(status)
  }

  override val downloadTasksSupported: Boolean
    get() = true

  override val downloadStatus: PlayerSpineElementDownloadStatus
    get() = synchronized(this.statusLock, { this.statusNow })

  override val id: String
    get() = String.format("%s-%d", this.bookID.value, this.index)
}
