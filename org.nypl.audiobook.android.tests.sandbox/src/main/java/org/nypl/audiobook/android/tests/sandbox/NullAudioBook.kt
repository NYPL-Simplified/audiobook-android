package org.nypl.audiobook.android.tests.sandbox

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.SortedMap
import java.util.concurrent.ExecutorService

/**
 * A fake audio book.
 */

class NullAudioBook(
  override val id: PlayerBookID,
  val downloadStatusExecutor: ExecutorService,
  val downloadProvider: PlayerDownloadProviderType,
  val player: NullPlayer) : PlayerAudioBookType {

  val statusEvents: BehaviorSubject<PlayerSpineElementDownloadStatus> = BehaviorSubject.create()
  val spineItems: MutableList<NullSpineElement> = mutableListOf()

  fun createSpineElement(id: String, title: String, duration: Duration): NullSpineElement {
    val element = NullSpineElement(
      bookNull = this,
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

  override val supportsStreaming: Boolean
    get() = false

  override val spine: List<PlayerSpineElementType>
    get() = this.spineItems

  override val spineByID: Map<String, PlayerSpineElementType>
    get() = this.spineItems.associateBy(keySelector = { e -> e.id }, valueTransform = { e -> e })

  override val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>
    get() = sortedMapOf()

  override val spineElementDownloadStatus: Observable<PlayerSpineElementDownloadStatus>
    get() = this.statusEvents

  override fun createPlayer(): PlayerType {
    return this.player
  }
}