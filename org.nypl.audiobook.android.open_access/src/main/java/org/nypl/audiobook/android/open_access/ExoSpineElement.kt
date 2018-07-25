package org.nypl.audiobook.android.open_access

import net.jcip.annotations.GuardedBy
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerDownloadTaskType
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementType
import rx.subjects.PublishSubject

/**
 * A spine element in an audio book.
 */

class ExoSpineElement(
  private val downloadStatusEvents: PublishSubject<PlayerSpineElementDownloadStatus>,
  val bookManifest: ExoManifest,
  val itemManifest: ExoManifestSpineItem,
  override val index: Int,
  internal var nextElement: PlayerSpineElementType?,
  override val duration: Duration)
  : PlayerSpineElementType {

  /**
   * The current download status of the spine element.
   */

  private val statusLock: Any = Object()
  @GuardedBy("statusLock")
  private var statusNow: PlayerSpineElementDownloadStatus = PlayerSpineElementNotDownloaded

  private lateinit var bookActual: ExoAudioBook

  override val book: PlayerAudioBookType
    get() = this.bookActual

  override val next: PlayerSpineElementType?
    get() = this.nextElement

  override val position: PlayerPosition
    get() = PlayerPosition(
      this.itemManifest.title,
      this.itemManifest.part,
      this.itemManifest.chapter,
      0)

  override val title: String
    get() = this.itemManifest.title

  override val downloadTask: PlayerDownloadTaskType =
    TODO("Download tasks have not yet been implemented")

  fun setBook(book: ExoAudioBook) {
    this.bookActual = book
  }

  fun setDownloadStatus(status: PlayerSpineElementDownloadStatus) {
    synchronized(this.statusLock, { this.statusNow = status })
    this.downloadStatusEvents.onNext(status)
  }

  override val downloadStatus: PlayerSpineElementDownloadStatus
    get() = synchronized(this.statusLock, { this.statusNow })

  override val id: String
    get() = String.format("%d-%d", this.itemManifest.part, this.itemManifest.chapter)
}