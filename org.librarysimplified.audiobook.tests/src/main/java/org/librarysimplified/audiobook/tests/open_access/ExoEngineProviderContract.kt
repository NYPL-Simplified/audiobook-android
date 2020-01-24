package org.librarysimplified.audiobook.tests.open_access

import android.content.Context
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import org.joda.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterCompleted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterWaiting
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackBuffering
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStarted
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.open_access.ExoEngineProvider
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.librarysimplified.audiobook.tests.DishonestDownloadProvider
import org.librarysimplified.audiobook.tests.ResourceDownloadProvider
import org.slf4j.Logger
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Tests for the {@link org.librarysimplified.audiobook.open_access.ExoEngineProvider} type.
 */

abstract class ExoEngineProviderContract {

  abstract fun log(): Logger

  abstract fun context(): Context

  private lateinit var exec: ListeningExecutorService
  private lateinit var timeThen: Instant
  private lateinit var timeNow: Instant

  @Rule
  @JvmField
  val expectedException: ExpectedException = ExpectedException.none()

  @Before
  open fun setup() {
    this.log().debug("setup")
    this.timeThen = Instant.now()
    this.exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1))!!
  }

  @After
  fun tearDown() {
    this.log().debug("tearDown")
    this.exec.shutdown()
    this.timeNow = Instant.now()
    this.log().debug("time: {}", Duration(this.timeThen, this.timeNow).standardSeconds)
  }

  /**
   * Test that the engine accepts the minimal example book.
   */

  @Test
  fun testAudioEnginesTrivial() {
    val manifest = this.parseManifest("ok_minimal_0.json")
    val request = PlayerAudioEngineRequest(manifest, filter = { true }, downloadProvider = DishonestDownloadProvider())
    val engine_provider = ExoEngineProvider()
    val book_provider = engine_provider.tryRequest(request)
    Assert.assertNotNull("Engine must handle manifest", book_provider)
    val book_provider_nn = book_provider!!
    val result = book_provider_nn.create(this.context())
    this.log().debug("testAudioEnginesTrivial:result: {}", result)
    Assert.assertTrue("Engine accepts book", result is PlayerResult.Success)
  }

  /**
   * Test that the engine accepts the flatland book.
   */

  @Test
  fun testAudioEnginesFlatland() {
    val manifest = this.parseManifest("flatland.audiobook-manifest.json")
    val request = PlayerAudioEngineRequest(manifest, filter = { true }, downloadProvider = DishonestDownloadProvider())
    val engine_provider = ExoEngineProvider()
    val book_provider = engine_provider.tryRequest(request)
    Assert.assertNotNull("Engine must handle manifest", book_provider)
    val book_provider_nn = book_provider!!
    val result = book_provider_nn.create(this.context())
    this.log().debug("testAudioEnginesFlatland:result: {}", result)
    Assert.assertTrue("Engine accepts book", result is PlayerResult.Success)
  }

  /**
   * Test that the player does not support streaming.
   */

  @Test
  fun testPlayNoStreaming() {
    val book = this.createBook("ok_minimal_0.json")
    Assert.assertFalse("Player does not support streaming", book.supportsStreaming)
  }

  /**
   * Test that the player reports closure.
   */

  @Test
  fun testPlayerOpenClose() {
    val book = this.createBook("ok_minimal_0.json")

    val player = book.createPlayer()
    Assert.assertFalse("Player is open", player.isClosed)
    player.close()
    Assert.assertTrue("Player is closed", player.isClosed)
  }

  /**
   * Test that trying to play a closed player is an error.
   */

  @Test
  fun testPlayerClosedPlay() {
    val book = this.createBook("ok_minimal_0.json")

    val player = book.createPlayer()
    Assert.assertFalse("Player is open", player.isClosed)
    player.close()

    this.expectedException.expect(IllegalStateException::class.java)
    player.play()
  }

  /**
   * Test that if a spine element isn't downloaded, and a request is made to play that spine
   * element, the player will publish a "waiting" event and stop playback.
   */

  @Test(timeout = 20_000L)
  fun testPlayerWaitingForChapter() {
    val book = this.createBook("ok_minimal_0.json")

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    player.play()
    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertEquals(2, events.size)
    Assert.assertEquals("playbackChapterWaiting 0", events[0])
    Assert.assertEquals("playbackStopped 0 0", events[1])
  }

  /**
   * Test that if the player is waiting for a particular spine element to be downloaded, that it
   * starts playback when the spine element becomes available.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayWhenDownloaded() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"),
              { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    Thread.sleep(1000L)

    player.play()
    this.downloadSpineItemAndWait(book.spine[0])
    Thread.sleep(1000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertEquals(7, events.size)
    Assert.assertEquals("playbackChapterWaiting 0", events[0])
    Assert.assertEquals("playbackStopped 0 0", events[1])
    Assert.assertEquals("rateChanged NORMAL_TIME", events[2])
    Assert.assertEquals("playbackStarted 0 0", events[3])
    Assert.assertTrue(events[4], events[4].startsWith("playbackProgressUpdate 0 "))
    Assert.assertTrue(events[5], events[5].startsWith("playbackProgressUpdate 0 "))
    Assert.assertEquals("playbackStopped 0 0", events[6])
  }

  private fun subscribeToEvents(player: PlayerType, events: ArrayList<String>, waitLatch: CountDownLatch) {
    player.events.subscribe(
      { event -> events.add(this.eventToString(event)) },
      { waitLatch.countDown() },
      { waitLatch.countDown() })
  }

  /**
   * Test that the player can play downloaded spine elements.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayAlreadyDownloaded() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"),
              { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    Thread.sleep(1000L)

    player.play()
    Thread.sleep(1000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertEquals(3, events.size)
    Assert.assertEquals("rateChanged NORMAL_TIME", events[0])
    Assert.assertEquals("playbackStarted 0 0", events[1])
    Assert.assertEquals("playbackStopped 0 0", events[2])
  }

  /**
   * Test that the player can play and pause.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayPausePlay() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"),
              { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    Thread.sleep(1000L)

    player.play()
    Thread.sleep(250L)

    player.pause()
    Thread.sleep(250L)

    player.play()
    Thread.sleep(250L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertEquals(5, events.size)
    Assert.assertEquals("rateChanged NORMAL_TIME", events[0])
    Assert.assertEquals("playbackStarted 0 0", events[1])
    Assert.assertEquals("playbackPaused 0 0", events[2])
    Assert.assertEquals("playbackStarted 0 0", events[3])
    Assert.assertEquals("playbackStopped 0 0", events[4])
  }

  /**
   * Test that the player automatically progresses across chapters.
   */

  @Test(timeout = 40_000L)
  fun testPlayerPlayNextChapter() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"), { this.resource("noise.mp3") }),
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3"), { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    book.spine[1].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    this.downloadSpineItemAndWait(book.spine[1])
    Thread.sleep(1000L)

    player.play()
    Thread.sleep(12_000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertTrue("At least 15 events must be logged (${events.size})", events.size >= 15)
    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 0 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 0")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 0", events.removeAt(0))
    Assert.assertEquals("playbackStopped 0 0", events.removeAt(0))

    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 1 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 1")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 1", events.removeAt(0))
    Assert.assertEquals("playbackStopped 1 0", events.removeAt(0))

    Assert.assertEquals("playbackChapterWaiting 2", events.removeAt(0))
    Assert.assertEquals("playbackStopped 2 0", events.removeAt(0))
  }

  /**
   * Test that deleting a spine element in the middle of playback stops playback.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayDeletePlaying() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"),
              { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    Thread.sleep(1000L)

    player.play()
    Thread.sleep(1000L)
    book.spine[0].downloadTask().delete()
    Thread.sleep(2000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertEquals(3, events.size)
    Assert.assertEquals("rateChanged NORMAL_TIME", events[0])
    Assert.assertEquals("playbackStarted 0 0", events[1])
    Assert.assertEquals("playbackStopped 0 0", events[2])
  }

  /**
   * Test that the skipping to the next chapter works.
   */

  @Test(timeout = 20_000L)
  fun testPlayerSkipNext() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"), { this.resource("noise.mp3") }),
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3"), { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    book.spine[1].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    this.downloadSpineItemAndWait(book.spine[1])

    player.play()
    player.skipToNextChapter()
    Thread.sleep(10_000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertTrue("At least 12 events must be logged (${events.size})", events.size >= 12)
    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 0 0", events.removeAt(0))
    Assert.assertEquals("playbackStopped 0 0", events.removeAt(0))

    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 1 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 1")) {
      events.removeAt(0)
    }

    Assert.assertEquals("playbackChapterCompleted 1", events.removeAt(0))
    Assert.assertEquals("playbackStopped 1 0", events.removeAt(0))

    Assert.assertEquals("playbackChapterWaiting 2", events.removeAt(0))
    Assert.assertEquals("playbackStopped 2 0", events.removeAt(0))
  }

  /**
   * Test that the skipping to the previous chapter works.
   */

  @Test(timeout = 20_000L)
  fun testPlayerSkipPrevious() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"), { this.resource("noise.mp3") }),
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3"), { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    book.spine[1].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    this.downloadSpineItemAndWait(book.spine[1])
    Thread.sleep(1000L)

    player.playAtLocation(book.spine[1].position)
    player.skipToPreviousChapter()
    Thread.sleep(12_000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertTrue("At least 16 events must be logged (${events.size})", events.size >= 16)
    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 1 0", events.removeAt(0))
    Assert.assertEquals("playbackStopped 1 0", events.removeAt(0))

    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 0 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 0")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 0", events.removeAt(0))
    Assert.assertEquals("playbackStopped 0 0", events.removeAt(0))

    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 1 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 1")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 1", events.removeAt(0))
    Assert.assertEquals("playbackStopped 1 0", events.removeAt(0))

    Assert.assertEquals("playbackChapterWaiting 2", events.removeAt(0))
    Assert.assertEquals("playbackStopped 2 0", events.removeAt(0))
  }

  /**
   * Test that playing a specific chapter works.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayAtLocation() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"), { this.resource("noise.mp3") }),
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3"), { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    book.spine[1].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    this.downloadSpineItemAndWait(book.spine[1])
    Thread.sleep(1000L)

    player.playAtLocation(book.spine[1].position)
    Thread.sleep(10_000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertTrue("At least 9 events must be logged (${events.size})", events.size >= 9)
    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 1 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 1")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 1", events.removeAt(0))
    Assert.assertEquals("playbackStopped 1 0", events.removeAt(0))

    Assert.assertEquals("playbackChapterWaiting 2", events.removeAt(0))
    Assert.assertEquals("playbackStopped 2 0", events.removeAt(0))
  }

  /**
   * Test that playing the start of the book works.
   */

  @Test(timeout = 20_000L)
  fun testPlayerPlayAtBookStart() {
    val book =
      this.createBook("flatland.audiobook-manifest.json",
        ResourceDownloadProvider.create(this.exec,
          mapOf(
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_1_abbott.mp3"), { this.resource("noise.mp3") }),
            Pair(URI.create("http://www.archive.org/download/flatland_rg_librivox/flatland_2_abbott.mp3"), { this.resource("noise.mp3") }))))

    val player = book.createPlayer()
    val waitLatch = CountDownLatch(1)
    val events = ArrayList<String>()
    this.subscribeToEvents(player, events, waitLatch)

    book.spine[0].downloadTask().delete()
    book.spine[1].downloadTask().delete()
    Thread.sleep(1000L)

    this.downloadSpineItemAndWait(book.spine[0])
    this.downloadSpineItemAndWait(book.spine[1])
    Thread.sleep(1000L)

    player.playAtBookStart()
    Thread.sleep(10_000L)

    player.close()
    waitLatch.await()

    this.showEvents(events)
    Assert.assertTrue("At least 9 events must be logged (${events.size})", events.size >= 9)
    Assert.assertEquals("rateChanged NORMAL_TIME", events.removeAt(0))
    Assert.assertEquals("playbackStarted 0 0", events.removeAt(0))
    while (events[0].startsWith("playbackProgressUpdate 0")) {
      events.removeAt(0)
    }
    Assert.assertEquals("playbackChapterCompleted 0", events.removeAt(0))
    Assert.assertEquals("playbackStopped 0 0", events.removeAt(0))
  }

  private fun showEvents(events: ArrayList<String>) {
    val log = this.log()
    log.debug("event count: {}", events.size)
    for (i in 0 until events.size) {
      log.debug("events[{}]: {}", i, events[i])
    }
  }

  private fun downloadSpineItemAndWait(spineItem: PlayerSpineElementType) {
    spineItem.downloadTask().delete()
    spineItem.downloadTask().fetch()

    var downloaded = false
    while (!downloaded) {
      val status = spineItem.downloadStatus
      this.log().debug("spine element status: {}", status)

      when (status) {
        is PlayerSpineElementNotDownloaded -> Unit
        is PlayerSpineElementDownloading -> Unit
        is PlayerSpineElementDownloaded -> downloaded = true
        is PlayerSpineElementDownloadFailed -> {
          this.log().error("error: ", status.exception)
          Assert.fail("Failed: " + status.message)
        }
      }

      Thread.sleep(1000L)
    }
  }

  private fun eventToString(event: PlayerEvent): String {
    return when (event) {
      is PlayerEventPlaybackRateChanged ->
        "rateChanged ${event.rate}"
      is PlayerEventPlaybackStarted ->
        "playbackStarted ${event.spineElement.index} ${event.offsetMilliseconds}"
      is PlayerEventPlaybackBuffering ->
        "playbackBuffering ${event.spineElement.index} ${event.offsetMilliseconds}"
      is PlayerEventPlaybackProgressUpdate ->
        "playbackProgressUpdate ${event.spineElement.index} ${event.offsetMilliseconds} ${event.offsetMilliseconds}"
      is PlayerEventChapterCompleted ->
        "playbackChapterCompleted ${event.spineElement.index}"
      is PlayerEventChapterWaiting ->
        "playbackChapterWaiting ${event.spineElement.index}"
      is PlayerEventPlaybackPaused ->
        "playbackPaused ${event.spineElement.index} ${event.offsetMilliseconds}"
      is PlayerEventPlaybackStopped ->
        "playbackStopped ${event.spineElement.index} ${event.offsetMilliseconds}"
      is PlayerEvent.PlayerEventError ->
        "playbackError ${event.spineElement?.index} ${event.exception?.javaClass?.canonicalName} ${event.errorCode} ${event.offsetMilliseconds}"
    }
  }

  private fun createBook(
    name: String,
    downloadProvider: PlayerDownloadProviderType = DishonestDownloadProvider()): PlayerAudioBookType {

    val manifest = this.parseManifest(name)
    val request = PlayerAudioEngineRequest(manifest, filter = { true }, downloadProvider = downloadProvider)
    val engine_provider = ExoEngineProvider()
    val book_provider = engine_provider.tryRequest(request)
    Assert.assertNotNull("Engine must handle manifest", book_provider)
    val book_provider_nn = book_provider!!
    val result = book_provider_nn.create(this.context())
    this.log().debug("testAudioEnginesTrivial:result: {}", result)

    val book = (result as PlayerResult.Success).result
    return book
  }

  private fun parseManifest(file: String): PlayerManifest {
    val result =
      ManifestParsers.parse(
        uri = URI.create("urn:$file"),
        streams = { this.resource(file) }
      )
    this.log().debug("parseManifest: result: {}", result)
    Assert.assertTrue("Result is success", result is ParseResult.Success)
    val manifest = (result as ParseResult.Success).result
    return manifest
  }

  private fun resource(name: String): InputStream {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return ExoEngineProviderContract::class.java.getResourceAsStream(path)
      ?: throw AssertionError("Missing resource file: " + path)
  }

}
