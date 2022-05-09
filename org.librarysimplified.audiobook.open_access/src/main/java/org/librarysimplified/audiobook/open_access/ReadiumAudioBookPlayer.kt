package org.librarysimplified.audiobook.open_access

import android.content.Context
import androidx.media2.common.SessionPlayer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.NORMAL_TIME
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerResult
import org.librarysimplified.audiobook.api.PlayerType
import org.readium.navigator.media2.ExoPlayerDataSource
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * A player based on the Readium media2 navigator.
 */

@OptIn(ExperimentalMedia2::class, ExperimentalTime::class)
class ReadiumAudioBookPlayer private constructor(
  private val statusEvents: BehaviorSubject<PlayerEvent>,
  private val book: ReadiumAudioBook,
  private val navigator: MediaNavigator,
) : PlayerType {

  private val coroutineScope: CoroutineScope = MainScope()
  private val log = LoggerFactory.getLogger(ReadiumAudioBookPlayer::class.java)
  private val closed = AtomicBoolean(false)

  init {
      navigator.playback
        .onEach(this::onPlaybackChanged)
        .launchIn(coroutineScope)
  }

  @Volatile
  private var currentPlaybackRate: PlayerPlaybackRate = NORMAL_TIME

  private fun currentSpineElement(): ReadiumSpineElement {
    return book.spine[navigator.playback.value.resource.index]
  }

  override val isPlaying: Boolean
    get() {
      this.checkNotClosed()
      return when (navigator.playback.value.state) {
        MediaNavigator.Playback.State.Playing -> true
        MediaNavigator.Playback.State.Paused -> false
        MediaNavigator.Playback.State.Finished -> false
        MediaNavigator.Playback.State.Error -> false
      }
    }

  private fun checkNotClosed() {
    if (this.closed.get()) {
      throw IllegalStateException("Player has been closed")
    }
  }

  override var playbackRate: PlayerPlaybackRate
    get() {
      this.checkNotClosed()
      return this.currentPlaybackRate
    }
    set(value) {
      this.checkNotClosed()
      coroutineScope.launch {
        navigator.setPlaybackRate(value.speed)
        currentPlaybackRate = value
        statusEvents.onNext(PlayerEventPlaybackRateChanged(value))
      }
    }

  override val events: Observable<PlayerEvent>
    get() {
      this.checkNotClosed()
      return this.statusEvents
    }

  override fun play() {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.play()
    }
  }

  override fun pause() {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.pause()
    }
  }

  override fun skipToNextChapter() {
    this.checkNotClosed()
    val currentChapter = navigator.playback.value.resource.index
    if (currentChapter < navigator.publication.readingOrder.size - 1) {
      coroutineScope.launch {
        navigator.seek(currentChapter + 1, Duration.ZERO)
      }
    }
  }

  override fun skipToPreviousChapter() {
    this.checkNotClosed()
    val currentChapter = navigator.playback.value.resource.index
    if (currentChapter > 0) {
      coroutineScope.launch {
        navigator.seek(currentChapter - 1, Duration.ZERO)
      }
    }
  }

  override fun skipPlayhead(milliseconds: Long) {
    this.checkNotClosed()
    coroutineScope.launch {
      if (milliseconds > 0) {
        navigator.goForward()
      } else if (milliseconds < 0) {
        navigator.goBackward()
      }
    }
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.seek(location.chapter, location.offsetMilliseconds.milliseconds)
      navigator.play()
    }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.seek(location.chapter, location.offsetMilliseconds.milliseconds)
    }
  }

  override fun playAtBookStart() {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.seek(0, Duration.ZERO)
      navigator.play()
    }
  }

  override fun movePlayheadToBookStart() {
    this.checkNotClosed()
    coroutineScope.launch {
      navigator.seek(0, Duration.ZERO)
    }
  }

  override val isClosed: Boolean
    get() = this.closed.get()

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.log.debug("opClose")
      this.navigator.close()
      this.statusEvents.onComplete()
    }
  }

  private fun onPlaybackChanged(playback: MediaNavigator.Playback) {
    return when (playback.state) {
      MediaNavigator.Playback.State.Playing -> {
        statusEvents.onNext(
          PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate(
            spineElement = this.book.spine[playback.resource.index],
            offsetMilliseconds = playback.resource.position.inWholeMilliseconds
          )
        )
      }
      MediaNavigator.Playback.State.Paused -> {
        statusEvents.onNext(
          PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused(
            spineElement = this.book.spine[playback.resource.index],
            offsetMilliseconds = playback.resource.position.inWholeMilliseconds
          )
        )
      }
      MediaNavigator.Playback.State.Finished -> {
        statusEvents.onNext(
          PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped(
            spineElement = this.book.spine[playback.resource.index],
            offsetMilliseconds = playback.resource.position.inWholeMilliseconds
          )
        )
      }
      MediaNavigator.Playback.State.Error -> {
        statusEvents.onNext(
          PlayerEvent.PlayerEventError(
            spineElement = currentSpineElement(),
            offsetMilliseconds = playback.resource.position.inWholeMilliseconds,
            exception = null,
            errorCode = 0
          )
        )
      }
    }
  }

  companion object {

    fun create(
      book: ReadiumAudioBook,
      publication: Publication,
      context: Context,
    ): PlayerResult<PlayerType, Exception> {

      val statusEvents =
        BehaviorSubject.create<PlayerEvent>()

      val playerTry = runBlocking(Dispatchers.Main) {
        MediaNavigator.create(
          context = context,
          publication = publication,
          initialLocator = null,
          player = createPlayer(context, publication, book.id),
          configuration = MediaNavigator.Configuration(
            skipForwardInterval = 15.seconds,
            skipBackwardInterval = 15.seconds,
          )
        )
      }.map {
        ReadiumAudioBookPlayer(
          book = book,
          navigator = it,
          statusEvents = statusEvents,
        )
      }

      return when (playerTry) {
        is Try.Success -> {
          PlayerResult.Success(playerTry.value)
        }
        is Try.Failure -> {
          PlayerResult.Failure(playerTry.exception)
        }
      }
    }

    private fun findDirectoryFor(context: Context, id: PlayerBookID): File {
      val base = context.filesDir
      val all = File(base, "exoplayer_audio")
      return File(all, id.value)
    }

    private fun createPlayer(context: Context, publication: Publication, bookID: PlayerBookID): SessionPlayer {
      val directory = this.findDirectoryFor(context, bookID)

      val cache = SimpleCache(
        directory,
        NoOpCacheEvictor(),
        StandaloneDatabaseProvider(context)
      )

      val publicationDataSource = ExoPlayerDataSource.Factory(publication)

      val dataSourceFactory =
        CacheDataSource.Factory()
          .setCache(cache)
          .setUpstreamDataSourceFactory(publicationDataSource)
          // Disable writing to the cache by the player. We'll handle downloads through the
          // service.
          .setCacheWriteDataSinkFactory(null)

      val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build(),
          true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

      return SessionPlayerConnector(player)
    }
  }
}
