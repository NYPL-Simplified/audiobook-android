package org.nypl.audiobook.android.mocking

import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.*
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject

/**
 * A player that does nothing.
 */

class MockingPlayer(private val book: MockingAudioBook) : PlayerType {

  private val log = LoggerFactory.getLogger(MockingPlayer::class.java)

  private val callEvents = PublishSubject.create<String>()
  private val statusEvents = BehaviorSubject.create<PlayerEvent>()

  val calls: Observable<String> = this.callEvents

  override fun close() {
    this.log.debug("close")
    this.callEvents.onNext("close")
  }

  override val isPlaying: Boolean
    get() = false

  override var playbackRate: PlayerPlaybackRate
    get() = PlayerPlaybackRate.NORMAL_TIME
    set(value) {
      this.log.debug("playbackRate {}", value)
      this.callEvents.onNext("playbackRate $value")
    }

  override val isClosed: Boolean
    get() = false

  override val events: Observable<PlayerEvent>
    get() = this.statusEvents

  fun error(
    exception: Exception?,
    errorCode: Int) {

    this.statusEvents.onNext(PlayerEvent.PlayerEventError(
      spineElement = null,
      offsetMilliseconds = 0,
      exception = exception,
      errorCode = errorCode))
  }

  override fun play() {
    this.log.debug("play")
    this.callEvents.onNext("play")
  }

  override fun pause() {
    this.log.debug("pause")
    this.callEvents.onNext("pause")
  }

  override fun skipToNextChapter() {
    this.log.debug("skipToNextChapter")
    this.callEvents.onNext("skipToNextChapter")
  }

  override fun skipToPreviousChapter() {
    this.log.debug("skipToPreviousChapter")
    this.callEvents.onNext("skipToPreviousChapter")
  }

  override fun skipForward() {
    this.log.debug("skipForward")
    this.callEvents.onNext("skipForward")
  }

  override fun skipBack() {
    this.log.debug("skipBack")
    this.callEvents.onNext("skipBack")
  }

  override fun skipPlayhead(milliseconds: Long) {
    this.log.debug("skipPlayhead {}", milliseconds)
    this.callEvents.onNext("skipPlayhead $milliseconds")
  }

  override fun playAtLocation(location: PlayerPosition) {
    this.log.debug("playAtLocation {} {} {}", location.part, location.chapter, location.offsetMilliseconds)
    this.callEvents.onNext("playAtLocation ${location.part} ${location.chapter} ${location.offsetMilliseconds}")
    this.goToChapter(location.chapter)
  }

  private fun goToChapter(chapter: Int) {
    val element = this.book.spineItems.find {
      element -> element.position.chapter == chapter
    }
    if (element != null) {
      this.statusEvents.onNext(PlayerEventPlaybackStarted(element, 0))
    }
  }

  override fun movePlayheadToLocation(location: PlayerPosition) {
    this.log.debug("movePlayheadToLocation {} {} {}", location.part, location.chapter, location.offsetMilliseconds)
    this.callEvents.onNext("movePlayheadToLocation ${location.part} ${location.chapter} ${location.offsetMilliseconds}")
    this.goToChapter(location.chapter)
  }
}