package org.librarysimplified.audiobook.open_access

import net.jcip.annotations.GuardedBy
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadTaskType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import java.lang.UnsupportedOperationException

/**
 * A spine element in an audio book.
 */

class ReadiumSpineElement(
  private val link: PlayerManifestLink,
  private val bookID: PlayerBookID,
  override val index: Int,
  internal var nextElement: PlayerSpineElementType?,
  internal var previousElement: PlayerSpineElementType?,
  @Volatile override var duration: Duration?,
) : PlayerSpineElementType {

  /**
   * The current download status of the spine element.
   */

  private val statusLock: Any = Object()
  @GuardedBy("statusLock")
  private var statusNow: PlayerSpineElementDownloadStatus =
    PlayerSpineElementNotDownloaded(this)

  private lateinit var bookActual: ReadiumAudioBook

  override val book: PlayerAudioBookType
    get() = this.bookActual

  override val next: PlayerSpineElementType?
    get() = this.nextElement

  override val previous: PlayerSpineElementType?
    get() = this.previousElement

  override val title: String?
    get() = this.link.title

  override fun downloadTask(): PlayerDownloadTaskType {
    throw UnsupportedOperationException()
  }

  fun setBook(book: ReadiumAudioBook) {
    this.bookActual = book
  }

  override val downloadTasksSupported: Boolean
    get() = true

  override val downloadStatus: PlayerSpineElementDownloadStatus
    get() = synchronized(this.statusLock) { this.statusNow }

  override val id: String
    get() = String.format("%s-%d", this.bookID.value, this.index)
}
