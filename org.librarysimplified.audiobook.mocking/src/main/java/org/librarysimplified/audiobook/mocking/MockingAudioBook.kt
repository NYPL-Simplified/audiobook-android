package org.librarysimplified.audiobook.mocking

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadWholeBookTaskType
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.SortedMap
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
      title = title)
    this.spineItems.add(element)
    return element
  }

  override var supportsStreaming: Boolean = false

  override val supportsIndividualChapterDeletion: Boolean
    get() = true

  override val spine: List<PlayerSpineElementType>
    get() = this.spineItems

  override val spineByID: Map<String, PlayerSpineElementType>
    get() = this.spineItems.associateBy(keySelector = { e -> e.id }, valueTransform = { e -> e })

  override val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>
    get() = sortedMapOf()

  override val spineElementDownloadStatus: Observable<PlayerSpineElementDownloadStatus>
    get() = this.statusEvents

  override val wholeBookDownloadTask: PlayerDownloadWholeBookTaskType
    get() = this.wholeTask

  override fun replaceManifest(
    manifest: PlayerManifest
  ): ListenableFuture<Unit> {
    val future = SettableFuture.create<Unit>()
    future.set(Unit)
    return future
  }

  override fun createPlayer(): MockingPlayer {
    check(!this.isClosed) { "Audio book has been closed" }

    return this.players.invoke(this)
  }

  override fun close() {
    if (this.isClosedNow.compareAndSet(false, true)) {
      // No resources to clean up
    }
  }

  override val isClosed: Boolean
    get() = this.isClosedNow.get()
}
