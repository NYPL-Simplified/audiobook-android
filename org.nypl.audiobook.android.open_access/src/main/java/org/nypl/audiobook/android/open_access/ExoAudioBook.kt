package org.nypl.audiobook.android.open_access

import android.content.Context
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory
import rx.subjects.PublishSubject
import java.io.File
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ScheduledExecutorService

/**
 * An ExoPlayer audio book.
 */

class ExoAudioBook private constructor(
  private val manifest: ExoManifest,
  private val exoPlayer: ExoAudioBookPlayer,
  override val spine: List<ExoSpineElement>,
  override val spineByID: Map<String, ExoSpineElement>,
  override val spineByPartAndChapter: SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>,
  override val spineElementDownloadStatus: PublishSubject<PlayerSpineElementDownloadStatus>,
  override val id: PlayerBookID,
  engineProvider: ExoEngineProvider)
  : PlayerAudioBookType {

  override val supportsStreaming: Boolean
    get() = false

  override val player: PlayerType
    get() = this.exoPlayer

  companion object {

    private val log = LoggerFactory.getLogger(ExoAudioBook::class.java)

    private fun findDirectoryFor(context: Context, id: PlayerBookID): File {
      val base = context.filesDir
      val all = File(base, "exoplayer_audio")
      return File(all, id.value)
    }

    fun create(
      engineProvider: ExoEngineProvider,
      context: Context,
      engineExecutor: ScheduledExecutorService,
      manifest: ExoManifest,
      downloadProvider: PlayerDownloadProviderType): PlayerAudioBookType {

      val bookId = PlayerBookID.transform(manifest.id)
      val directory = findDirectoryFor(context, bookId)
      this.log.debug("book directory: {}", directory)
      val player =
        ExoAudioBookPlayer.create(
          engineProvider = engineProvider,
          context = context,
          engineExecutor = engineExecutor)

      /*
       * Set up all the various bits of state required.
       */

      val statusEvents: PublishSubject<PlayerSpineElementDownloadStatus> = PublishSubject.create()
      val elements = ArrayList<ExoSpineElement>()
      val elementsById = HashMap<String, ExoSpineElement>()
      val elementsByPart = TreeMap<Int, TreeMap<Int, PlayerSpineElementType>>()

      var index = 0
      var spineItemPrevious: ExoSpineElement? = null
      manifest.spineItems.forEach { spine_item ->

        val duration =
          Duration.standardSeconds(Math.floor(spine_item.duration).toLong())
        val partFile =
          File(directory, "${index}.part")

        val element =
          ExoSpineElement(
            downloadStatusEvents = statusEvents,
            bookID = bookId,
            bookManifest = manifest,
            itemManifest = spine_item,
            partFile = partFile,
            downloadProvider = downloadProvider,
            index = index,
            nextElement = null,
            previousElement = spineItemPrevious,
            duration = duration,
            engineExecutor = engineExecutor)

        elements.add(element)
        elementsById.put(element.id, element)
        this.addElementByPartAndChapter(elementsByPart, element)
        ++index

        /*
         * Make the "next" field of the previous element point to the current element.
         */

        val previous = spineItemPrevious
        if (previous != null) {
          previous.nextElement = element
        }
        spineItemPrevious = element
      }

      val book = ExoAudioBook(
        engineProvider = engineProvider,
        id = bookId,
        manifest = manifest,
        exoPlayer = player,
        spine = elements,
        spineByID = elementsById,
        spineByPartAndChapter = elementsByPart as SortedMap<Int, SortedMap<Int, PlayerSpineElementType>>,
        spineElementDownloadStatus = statusEvents)

      for (e in elements) {
        e.setBook(book)
      }

      player.setBook(book)
      return book
    }

    /**
     * Organize an element by part number and chapter number.
     */

    private fun addElementByPartAndChapter(
      elementsByPart: TreeMap<Int, TreeMap<Int, PlayerSpineElementType>>,
      element: ExoSpineElement) {

      val partChapters: TreeMap<Int, PlayerSpineElementType> =
        if (elementsByPart.containsKey(element.itemManifest.part)) {
          elementsByPart[element.itemManifest.part]!!
        } else {
          TreeMap()
        }

      partChapters.put(element.itemManifest.chapter, element)
      elementsByPart.put(element.itemManifest.part, partChapters)
    }
  }
}
