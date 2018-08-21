package org.nypl.audiobook.android.api

import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerSleepTimer.PlayerTimerRequest.PlayerTimerRequestClose
import org.nypl.audiobook.android.api.PlayerSleepTimer.PlayerTimerRequest.PlayerTimerRequestStart
import org.nypl.audiobook.android.api.PlayerSleepTimer.PlayerTimerRequest.PlayerTimerRequestStop
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerCancelled
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerFinished
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerRunning
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerStopped
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.ThreadSafe

/**
 * The primary implementation of the {@link PlayerSleepTimerType} interface.
 *
 * The implementation is thread-safe, and a given instance may be used from any thread.
 */

@ThreadSafe
class PlayerSleepTimer private constructor(
  private val statusEvents: BehaviorSubject<PlayerSleepTimerEvent>,
  private val executor: ExecutorService)
  : PlayerSleepTimerType {

  /**
   * The type of requests that can be made to the timer.
   */

  private sealed class PlayerTimerRequest {

    /**
     * Request that the timer be closed.
     */

    object PlayerTimerRequestClose : PlayerTimerRequest()

    /**
     * Request that the timer start now and count down over the given duration. If the timer
     * is already running, the timer is restarted.
     */

    class PlayerTimerRequestStart(
      val duration: Duration)
      : PlayerTimerRequest()

    /**
     * Request that the timer stop.
     */

    object PlayerTimerRequestStop : PlayerTimerRequest()
  }

  private val log: Logger = LoggerFactory.getLogger(PlayerSleepTimer::class.java)
  private val closed: AtomicBoolean = AtomicBoolean(false)
  private var task: Future<*>
  private val requests: ArrayBlockingQueue<PlayerTimerRequest> = ArrayBlockingQueue(16)

  init {
    this.log.debug("starting initial task")
    this.task = this.executor.submit(PlayerSleepTimerTask(this))
  }

  /**
   * A sleep timer task that runs for as long as the sleep timer exists. The task is
   * terminated when the sleep timer is closed.
   */

  private class PlayerSleepTimerTask(private val timer: PlayerSleepTimer) : Runnable {

    private val log: Logger = LoggerFactory.getLogger(PlayerSleepTimerTask::class.java)
    private var remaining: Duration = Duration.ZERO
    private val oneSecond = Duration.standardSeconds(1L)

    init {
      this.log.debug("created timer task")
    }

    override fun run() {
      this.log.debug("starting main task")


      try {

        /*
         * Wait indefinitely (or at least until the thread is interrupted) for an initial
         * request.
         */

        initialRequestWaiting@ while (true) {
          this.log.debug("waiting for timer requests")
          this.timer.statusEvents.onNext(PlayerSleepTimerStopped)

          var initialRequest: PlayerTimerRequest?
          try {
            initialRequest = this.timer.requests.take()
          } catch (e: InterruptedException) {
            initialRequest = null
          }

          if (this.timer.isClosed) {
            this.onCloseNoticed()
            return
          }

          when (initialRequest) {
            null, PlayerTimerRequestClose -> {
              if (this.onCloseNoticed()) {
                return
              }
            }

            is PlayerTimerRequestStart -> {
              this.log.debug("received start request: {}", initialRequest.duration)
              this.remaining = initialRequest.duration
              this.timer.statusEvents.onNext(PlayerSleepTimerRunning(this.remaining))
            }

            PlayerTimerRequestStop -> {
              this.log.debug("received (redundant) stop request")
              this.timer.statusEvents.onNext(PlayerSleepTimerStopped)
              break@initialRequestWaiting
            }
          }

          /*
           * The timer is now running. Wait in a loop for requests. Time out waiting after a second
           * each time in order to decrement the remaining time.
           */

          processingTimerRequests@ while (true) {

            var request: PlayerTimerRequest?
            try {
              request = this.timer.requests.poll(1L, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
              request = null
            }

            when (request) {
              null -> {
                this.remaining = this.remaining.minus(this.oneSecond)
                this.timer.statusEvents.onNext(PlayerSleepTimerRunning(this.remaining))

                if (this.remaining.isShorterThan(this.oneSecond)) {
                  this.log.debug("timer finished")
                  this.timer.statusEvents.onNext(PlayerSleepTimerFinished)
                  break@initialRequestWaiting
                }
              }

              PlayerTimerRequestClose -> {
                if (this.onCloseNoticed()) {
                  return
                }
              }

              is PlayerTimerRequestStart -> {
                this.log.debug("restarting timer")
                this.remaining = request.duration
                this.timer.statusEvents.onNext(PlayerSleepTimerRunning(this.remaining))
                continue@processingTimerRequests
              }

              PlayerTimerRequestStop -> {
                this.log.debug("stopping timer")
                this.timer.statusEvents.onNext(PlayerSleepTimerCancelled(this.remaining))
                this.timer.statusEvents.onNext(PlayerSleepTimerStopped)
                break@initialRequestWaiting
              }
            }
          }
        }
      } finally {
        this.log.debug("stopping main task")

        if (!this.timer.isClosed) {
          this.log.debug("resubmitting main task")
          this.timer.task = this.timer.executor.submit(PlayerSleepTimerTask(this.timer))
        }
      }
    }

    private fun onCloseNoticed(): Boolean {
      this.log.debug("received close request")
      this.timer.statusEvents.onNext(PlayerSleepTimerStopped)
      this.timer.statusEvents.onCompleted()
      return true
    }
  }

  companion object {

    private val log: Logger = LoggerFactory.getLogger(PlayerSleepTimer::class.java)

    /**
     * Create a new sleep timer.
     */

    fun create(): PlayerSleepTimerType {
      return PlayerSleepTimer(
        executor = Executors.newFixedThreadPool(1) { run -> this.createTimerThread(run) },
        statusEvents = BehaviorSubject.create())
    }

    /**
     * Create a thread suitable for use with the ExoPlayer audio engine.
     */

    private fun createTimerThread(r: Runnable?): Thread {
      val thread = PlayerSleepTimerThread(r ?: Runnable { })
      this.log.debug("created timer thread: {}", thread.name)
      thread.setUncaughtExceptionHandler { t, e ->
        this.log.error("uncaught exception on engine thread {}: ", t, e)
      }
      return thread
    }
  }

  private fun checkIsNotClosed() {
    if (this.isClosed) {
      throw IllegalStateException("Timer has been closed")
    }
  }

  override fun start(time: Duration) {
    this.checkIsNotClosed()
    this.requests.offer(PlayerTimerRequestStart(time), 10L, TimeUnit.MILLISECONDS)
  }

  override fun cancel() {
    this.checkIsNotClosed()
    this.requests.offer(PlayerTimerRequestStop, 10L, TimeUnit.MILLISECONDS)
  }

  override fun close() {
    if (this.closed.compareAndSet(false, true)) {
      this.task.cancel(true)
      this.executor.shutdown()
      this.requests.offer(PlayerTimerRequestClose, 10L, TimeUnit.MILLISECONDS)
    }
  }

  override val isClosed: Boolean
    get() = this.closed.get()

  override val status: Observable<PlayerSleepTimerEvent>
    get() = this.statusEvents.distinctUntilChanged()

}
