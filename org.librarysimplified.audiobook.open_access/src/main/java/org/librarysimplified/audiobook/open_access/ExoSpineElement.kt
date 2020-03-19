package org.librarysimplified.audiobook.open_access

import net.jcip.annotations.GuardedBy
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadTaskType
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
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
  private val extensions: List<PlayerExtensionType>,
  private val downloadProvider: PlayerDownloadProviderType,
  override val index: Int,
  internal var nextElement: PlayerSpineElementType?,
  internal var previousElement: PlayerSpineElementType?,
  override val duration: Duration,
  val engineExecutor: ExecutorService
) : PlayerSpineElementType {

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
      spineElement = this,
      extensions = this.extensions
    )

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
