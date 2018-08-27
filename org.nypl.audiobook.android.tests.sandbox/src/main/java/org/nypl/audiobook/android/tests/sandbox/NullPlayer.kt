package org.nypl.audiobook.android.tests.sandbox

import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.audiobook.android.api.PlayerType
import rx.Observable

/**
 * A player that does nothing.
 */

class NullPlayer : PlayerType {

  override fun close() {

  }

  override val isPlaying: Boolean
    get() = false

  override var playbackRate: PlayerPlaybackRate
    get() = PlayerPlaybackRate.NORMAL_TIME
    set(value) {}

  override val isClosed: Boolean
    get() = false

  override val events: Observable<PlayerEvent>
    get() = Observable.empty()

  override fun play() {

  }

  override fun pause() {

  }

  override fun skipToNextChapter() {

  }

  override fun skipToPreviousChapter() {

  }

  override fun skipForward() {

  }

  override fun skipBack() {

  }

  override fun playAtLocation(location: PlayerPosition) {

  }

  override fun movePlayheadToLocation(location: PlayerPosition) {

  }

}