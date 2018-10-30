package org.nypl.audiobook.android.mocking

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.SortedMap
import java.util.concurrent.ExecutorService

/**
 * A fake audio book.
 */

class MockingAudioBook(
  override val id: PlayerBookID,
  val downloadStatusExecutor: ExecutorService,
  val downloadProvider: PlayerDownloadProviderType,
  val players: (MockingAudioBook) -> MockingPlayer) : PlayerAudioBookType {

  val statusEvents: BehaviorSubject<PlayerSpineElementDownloadStatus> = BehaviorSubject.create()
  val spineItems: MutableList<MockingSpineElement> = mutableListOf()

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

  override fun createPlayer(): MockingPlayer {
    return this.players.invoke(this)
  }
}