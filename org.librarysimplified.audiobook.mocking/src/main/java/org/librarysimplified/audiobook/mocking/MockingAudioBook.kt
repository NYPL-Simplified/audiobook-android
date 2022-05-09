package org.librarysimplified.audiobook.mocking

import io.reactivex.subjects.BehaviorSubject
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.api.PlayerType
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fake audio book.
 */

class MockingAudioBook(
  override val id: PlayerBookID,
  val downloadStatusExecutor: ExecutorService,
  val downloadProvider: PlayerDownloadProviderType,
  val players: (MockingAudioBook) -> MockingPlayer
) : PlayerAudioBookType {

  val statusEvents: BehaviorSubject<PlayerSpineElementDownloadStatus> = BehaviorSubject.create()
  val spineItems: MutableList<MockingSpineElement> = mutableListOf()

  private val isClosedNow = AtomicBoolean(false)
  private val wholeTask = MockingDownloadWholeBookTask(this)

  fun createSpineElement(id: String, title: String, duration: Duration): MockingSpineElement {
    val element = MockingSpineElement(
      bookMocking = this,
      downloadProvider = this.downloadProvider,
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadStatusEvents = this.statusEvents,
      index = spineItems.size,
      duration = duration,
      id = id,
      title = title
    )
    this.spineItems.add(element)
    return element
  }

  override var supportsStreaming: Boolean = false

  override val supportsIndividualChapterDeletion: Boolean
    get() = true

  override val spine: List<PlayerSpineElementType>
    get() = spineItems

  override fun createPlayer(): PlayerResult<PlayerType, Exception> {
    check(!this.isClosed) { "Audio book has been closed" }

    return PlayerResult.Success(this.players.invoke(this))
  }

  override fun close() {
    if (this.isClosedNow.compareAndSet(false, true)) {
      // No resources to clean up
    }
  }

  override val isClosed: Boolean
    get() = this.isClosedNow.get()
}
