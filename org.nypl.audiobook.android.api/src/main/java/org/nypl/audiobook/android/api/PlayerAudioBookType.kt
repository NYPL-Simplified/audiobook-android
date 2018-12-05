package org.nypl.audiobook.android.api

import rx.Observable
import java.util.SortedMap

/**
 * An instance of an audio book.
 */

interface PlayerAudioBookType {

  /**
   * A unique identifier for the book.
   */

  val id: PlayerBookID

  /**
   * True iff the underlying audio book supports streaming. That is, it's not necessary to download
   * a book part before it's possible to play that part.
   */

  val supportsStreaming: Boolean

  /**
   * True iff the underlying audio engine supports the deletion of individual chapters via
   * the PlayerDownloadTaskType interface. If this is false, local book data may only be
   * deleted via the `deleteLocalChapterData` method.
   */

  val supportsIndividualChapterDeletion: Boolean

  /**
   * The list of spine items in reading order.
   */

  val spine: List<PlayerSpineElementType>

  /**
   * The spine items organized by their unique IDs.
   */

  val spineByID: Map<String, PlayerSpineElementType>

  /**
   * The spine items grouped into parts and chapters.
   */

  val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>

  /**
   * A convenience method for accessing elements of the #spineByPartAndChapter property.
   */

  fun spineElementForPartAndChapter(
    part: Int,
    chapter: Int): PlayerSpineElementType? {
    if (this.spineByPartAndChapter.containsKey(part)) {
      val chapters = this.spineByPartAndChapter[part]!!
      return chapters[chapter]
    }
    return null
  }

  /**
   * An observable publishing changes to the current download status of the part.
   */

  val spineElementDownloadStatus: Observable<PlayerSpineElementDownloadStatus>

  /**
   * Create a player for the audio book. The player must be closed when it is no longer needed.
   */

  fun createPlayer(): PlayerType

  /**
   * Delete all local data for chapters (if any). This will also have the effect of
   * cancelling any downloads that happen to be in progress.
   */

  fun deleteLocalChapterData() {
    this.spine.forEach { item -> item.downloadTask.delete() }
  }

  /**
   * A download task that downloads all chapters. Implementations are permitted to provide
   * a download task that uses a more efficient whole-book download procedure internally.
   */

  val wholeBookDownloadTask: PlayerDownloadWholeBookTaskType
}
