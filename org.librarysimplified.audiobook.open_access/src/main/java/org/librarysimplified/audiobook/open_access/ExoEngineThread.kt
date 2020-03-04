package org.librarysimplified.audiobook.open_access

import android.os.Looper
import org.slf4j.LoggerFactory

/**
 * An engine thread. The purpose of this class is to allow for efficient checks of the form
 * "is the current thread an engine thread?". Specifically, if the current thread is an instance
 * of ExoEngineThread, then the current thread is an engine thread. Instances of this class
 * guarantee that Looper.prepare() has been called before the given Runnable is executed as long
 * as they are created via the [create] method. For ease of unit testing when not running on
 * real Android implementations, the [createWithoutPreparation] method can be used.
 */

class ExoEngineThread private constructor(
  runnable: Runnable
) : Thread(runnable) {

  init {
    this.name = "org.librarysimplified.audiobook.open_access:engine:${this.id}"
  }

  companion object {

    private val logger = LoggerFactory.getLogger(ExoEngineThread::class.java)

    fun createWithoutPreparation(
      runnable: Runnable
    ): ExoEngineThread {
      val thread = ExoEngineThread(runnable)
      thread.setUncaughtExceptionHandler { t, e ->
        this.logger.error("uncaught exception on engine thread {}: ", t, e)
      }
      return thread
    }

    fun create(
      runnable: Runnable
    ): ExoEngineThread {
      val thread = ExoEngineThread(
        Runnable {
          Looper.prepare()
          runnable.run()
        }
      )

      thread.setUncaughtExceptionHandler { t, e ->
        this.logger.error("uncaught exception on engine thread {}: ", t, e)
      }
      return thread
    }

    fun isExoEngineThread(): Boolean {
      return Thread.currentThread() is ExoEngineThread
    }

    fun checkIsExoEngineThread() {
      if (!this.isExoEngineThread()) {
        throw IllegalStateException(
          StringBuilder(128)
            .append("Current thread is not an engine thread!\n")
            .append("  Thread: ")
            .append(Thread.currentThread())
            .append('\n')
            .toString())
      }
    }
  }
}
