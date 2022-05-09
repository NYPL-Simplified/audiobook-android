package org.librarysimplified.audiobook.open_access

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.r2.shared.fetcher.HttpFetcher
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Href
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A Readium audio book.
 */

@OptIn(ExperimentalMedia2::class)
class ReadiumAudioBook private constructor(
  private val context: Context,
  private val publication: Publication,
  override val spine: List<ReadiumSpineElement>,
  override val id: PlayerBookID,
) : PlayerAudioBookType {

  private val logger = LoggerFactory.getLogger(ReadiumAudioBook::class.java)
  private val isClosedNow = AtomicBoolean(false)

  override fun createPlayer(): PlayerResult<PlayerType, Exception> {
    check(!this.isClosed) { "Audio book has been closed" }

    return ReadiumAudioBookPlayer.create(
      book = this,
      context = this.context,
      publication = this.publication
    )
  }

  override val supportsStreaming: Boolean
    get() = true

  override val supportsIndividualChapterDeletion: Boolean
    get() = false

  companion object {

    private val log = LoggerFactory.getLogger(ReadiumAudioBook::class.java)

    private fun findDirectoryFor(context: Context, id: PlayerBookID): File {
      val base = context.filesDir
      val all = File(base, "exoplayer_audio")
      return File(all, id.value)
    }

    fun create(
      context: Context,
      manifest: PlayerManifest,
      downloadManifest: () -> PlayerManifest
    ): PlayerAudioBookType {
      val bookId = PlayerBookID.transform(manifest.metadata.identifier)
      val directory = this.findDirectoryFor(context, bookId)
      this.log.debug("book directory: {}", directory)

      /*
       * Set up all the various bits of state required.
       */

      val elements = ArrayList<ReadiumSpineElement>()
      val elementsById = HashMap<String, ReadiumSpineElement>()

      var index = 0
      var spineItemPrevious: ReadiumSpineElement? = null
      manifest.readingOrder.forEach { spineItem ->

        val duration =
          spineItem.duration?.let { time ->
            Duration.standardSeconds(Math.floor(time).toLong())
          }

        val element =
          ReadiumSpineElement(
            link = spineItem,
            bookID = bookId,
            index = index,
            nextElement = null,
            previousElement = spineItemPrevious,
            duration = duration,
          )

        elements.add(element)
        elementsById.put(element.id, element)
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

      val publication =
        createPublication(
          manifest, downloadManifest
        )

      val book =
        ReadiumAudioBook(
          context = context,
          id = bookId,
          publication = publication,
          spine = elements
        )

      for (e in elements) {
        e.setBook(book)
      }

      return book
    }

    private fun createPublication(playerManifest: PlayerManifest, downloadManifest: () -> PlayerManifest): Publication {

      val manifest =
        mapManifest(playerManifest)

      val downloadManifestExecutor =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

      val fetcher = UpdateManifestFetcher(
        childFetcher = HttpFetcher(
          client = DefaultHttpClient()
        ),
        downloadManifest = {
          downloadManifestSuspend(downloadManifest, downloadManifestExecutor)
        }
      )

      return Publication.Builder(
        manifest = manifest,
        fetcher = fetcher
      ).build()
    }

    private suspend fun downloadManifestSuspend(
      downloadManifest: () -> PlayerManifest,
      executor: ListeningExecutorService
    ): Try<Manifest, Exception> = suspendCoroutine { continuation ->
      val future = executor.submit {
        try {
          val playerManifest = downloadManifest()
          val readiumManifest = mapManifest(playerManifest)
          Try.success(readiumManifest)
        } catch (e: Exception) {
          Try.failure(e)
        }
      }

      future.addListener(
        { continuation.resume(future.get() as Try<Manifest, Exception>) },
        MoreExecutors.directExecutor()
      )
    }

    private fun mapManifest(playerManifest: PlayerManifest): Manifest =
      Manifest(
        metadata = org.readium.r2.shared.publication.Metadata(
          identifier = playerManifest.metadata.identifier,
          localizedTitle = LocalizedString(playerManifest.metadata.title)
        ),
        readingOrder = playerManifest.readingOrder
          .filterIsInstance(PlayerManifestLink.LinkBasic::class.java)
          .map { it.toLink() },
      )

    private fun PlayerManifestLink.toLink() =
      Link(
        href = Href(hrefURI.toString()).string,
        type = type?.toString()
      )
  }

  override fun close() {
    if (this.isClosedNow.compareAndSet(false, true)) {
      this.logger.debug("closed audio book")
    }
  }

  override val isClosed: Boolean
    get() = this.isClosedNow.get()
}
