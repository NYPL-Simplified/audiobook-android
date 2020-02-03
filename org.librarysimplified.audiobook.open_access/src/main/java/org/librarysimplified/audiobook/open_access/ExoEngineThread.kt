package org.librarysimplified.audiobook.open_access

import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An engine thread. The purpose of this class is to allow for efficient checks of the form
 * "is the current thread an engine thread?". Specifically, if the current thread is an instance
 * of ExoEngineThread, then the current thread is an engine thread. Instances of this class
 * guarantee that Looper.prepare() has been called before the given Runnable is executed.
 */

class ExoEngineThread(
  runnable: Runnable,
  private val prepared: AtomicBoolean = AtomicBoolean(false)
) : Thread(Runnable {
  if (prepared.compareAndSet(false, true)) {
    Looper.prepare()
  }
  runnable.run()
}) {

  init {
    this.name = "org.librarysimplified.audiobook.open_access:engine:${this.id}"
  }

  companion object {

    fun isExoEngineThread(): Boolean {
      return Thread.currentThread() is ExoEngineThread
    }

    fun checkIsExoEngineThread() {
      if (!isExoEngineThread()) {
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
