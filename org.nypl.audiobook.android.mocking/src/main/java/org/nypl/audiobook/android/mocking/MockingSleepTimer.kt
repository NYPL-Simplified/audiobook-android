package org.nypl.audiobook.android.mocking

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import rx.Observable

/**
 * A sleep timer that does nothing at all.
 */

class MockingSleepTimer : PlayerSleepTimerType {

  override fun start(time: Duration?) {

  }

  override fun cancel() {

  }

  override fun finish() {

  }

  override val status: Observable<PlayerSleepTimerEvent>
    get() = Observable.empty()

  override fun close() {

  }

  override val isClosed: Boolean
    get() = false

  override val isRunning: PlayerSleepTimerType.Running?
    get() = null

}