package org.librarysimplified.audiobook.mocking

import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerEvent.PlayerEventWithSpineElement.*
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerType
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
  private var rate: PlayerPlaybackRate = PlayerPlaybackRate.NORMAL_TIME

  val calls: Observable<String> = this.callEvents

  override fun close() {
    this.log.debug("close")
    this.callEvents.onNext("close")
  }

  override val isPlaying: Boolean
    get() = false

  override var playbackRate: PlayerPlaybackRate
    get() = this.rate
    set(value) {
      this.log.debug("playbackRate {}", value)
      this.callEvents.onNext("playbackRate $value")
      this.rate = value
      this.statusEvents.onNext(PlayerEvent.PlayerEventPlaybackRateChanged(value))
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

  fun buffering() {
    this.statusEvents.onNext(PlayerEventPlaybackBuffering(
      spineElement = this.book.spine.first(),
      offsetMilliseconds = 0L))
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

  override fun playAtBookStart() {
    this.log.debug("playAtBookStart")
    this.playAtLocation(this.book.spineItems.first().position)
  }

  override fun movePlayheadToBookStart() {
    this.log.debug("movePlayheadToBookStart")
    this.movePlayheadToLocation(this.book.spineItems.first().position)
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