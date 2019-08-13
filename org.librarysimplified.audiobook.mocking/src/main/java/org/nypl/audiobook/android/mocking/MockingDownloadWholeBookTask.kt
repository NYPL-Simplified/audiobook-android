package org.nypl.audiobook.android.mocking

import org.librarysimplified.audiobook.api.PlayerDownloadWholeBookTaskType
import org.slf4j.LoggerFactory

/**
 * A fake download task.
 */

class MockingDownloadWholeBookTask(private val audioBook: MockingAudioBook)
  : PlayerDownloadWholeBookTaskType {

  private val log = LoggerFactory.getLogger(MockingDownloadWholeBookTask::class.java)

  override fun fetch() {
    this.audioBook.spine.map { item -> item.downloadTask().fetch() }
  }

  override fun cancel() {
    this.audioBook.spine.map { item -> item.downloadTask().cancel() }
  }

  override fun delete() {
    this.audioBook.spine.map { item -> item.downloadTask().delete() }
  }

  override val progress: Double
    get() = calculateProgress()

  private fun calculateProgress(): Double {
    return this.audioBook.spine.sumByDouble { item -> item.downloadTask().progress } / this.audioBook.spine.size
  }

}
