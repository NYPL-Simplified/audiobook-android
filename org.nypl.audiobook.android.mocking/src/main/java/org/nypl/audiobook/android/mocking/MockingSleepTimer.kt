package org.nypl.audiobook.android.mocking

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import rx.Observable
import rx.subjects.BehaviorSubject

/**
 * A sleep timer that does nothing at all.
 */

class MockingSleepTimer : PlayerSleepTimerType {

  private val events = BehaviorSubject.create<PlayerSleepTimerEvent>()
  private var running: PlayerSleepTimerType.Running? = null
  private var closed: Boolean = false

  override fun start(time: Duration?) {
    this.running = PlayerSleepTimerType.Running(time)
    this.events.onNext(PlayerSleepTimerEvent.PlayerSleepTimerRunning(time))
  }

  override fun cancel() {
    this.events.onNext(PlayerSleepTimerEvent.PlayerSleepTimerCancelled(this.running?.duration))
  }

  override fun finish() {
    this.events.onNext(PlayerSleepTimerEvent.PlayerSleepTimerFinished)
  }

  override val status: Observable<PlayerSleepTimerEvent>
    get() = this.events

  override fun close() {
    this.closed = true
    this.events.onCompleted()
  }

  override val isClosed: Boolean
    get() = this.closed

  override val isRunning: PlayerSleepTimerType.Running?
    get() = this.running

}