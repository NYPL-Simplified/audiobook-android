package org.nypl.audiobook.android.open_access

import android.content.Context
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import rx.subjects.PublishSubject
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ExecutorService

/**
 * An ExoPlayer audio book.
 */

class ExoAudioBook private constructor(
  val manifest: ExoManifest,
  private val exoPlayer: ExoAudioBookPlayer,
  override val spine: List<ExoSpineElement>,
  override val spineByID: Map<String, ExoSpineElement>,
  override val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>,
  override val spineElementDownloadStatus: PublishSubject<PlayerSpineElementDownloadStatus>,
  override val id: PlayerBookID)
  : PlayerAudioBookType {

  override val supportsStreaming: Boolean
    get() = false

  override val player: PlayerType
    get() = this.exoPlayer

  companion object {

    fun create(
      context: Context,
      engineExecutor: ExecutorService,
      manifest: ExoManifest): PlayerAudioBookType {

      val book_id = PlayerBookID.transform(manifest.id)
      val player = ExoAudioBookPlayer.create(context, engineExecutor, book_id)

      /*
       * Set up all the various bits of state required.
       */

      val statusEvents: PublishSubject<PlayerSpineElementDownloadStatus> = PublishSubject.create()
      val elements = ArrayList<ExoSpineElement>()
      val elements_by_id = HashMap<String, ExoSpineElement>()
      val elements_by_part = TreeMap<Int, TreeMap<Int, PlayerSpineElementType>>()

      var index = 0
      var spine_item_previous: ExoSpineElement? = null
      manifest.spineItems.forEach { spine_item ->

        val duration =
          Duration.standardSeconds(Math.floor(spine_item.duration).toLong())

        val element =
          ExoSpineElement(
            downloadStatusEvents = statusEvents,
            itemManifest = spine_item,
            bookManifest = manifest,
            index = index,
            nextElement = null,
            duration = duration)

        elements.add(element)
        elements_by_id.put(element.id, element)
        this.addElementByPartAndChapter(elements_by_part, element)
        ++index

        /*
         * Make the "next" field of the previous element point to the current element.
         */

        val previous = spine_item_previous
        if (previous != null) {
          previous.nextElement = element
        }
        spine_item_previous = element
      }

      val book = ExoAudioBook(
        id = book_id,
        manifest = manifest,
        exoPlayer = player,
        spine = elements,
        spineByID = elements_by_id,
        spineByPartAndChapter = elements_by_part as SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>,
        spineElementDownloadStatus = statusEvents)

      for (e in elements) {
        e.setBook(book)
      }
      return book
    }

    /**
     * Organize an element by part number and chapter number.
     */

    private fun addElementByPartAndChapter(
      elements_by_part: TreeMap<Int, TreeMap<Int, PlayerSpineElementType>>,
      element: ExoSpineElement) {

      val part_chapters: TreeMap<Int, PlayerSpineElementType> =
        if (elements_by_part.containsKey(element.itemManifest.part)) {
          elements_by_part[element.itemManifest.part]!!
        } else {
          TreeMap()
        }

      part_chapters.put(element.itemManifest.chapter, element)
      elements_by_part.put(element.itemManifest.part, part_chapters)
    }
  }
}
