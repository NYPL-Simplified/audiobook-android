package org.librarysimplified.audiobook.mocking

import org.librarysimplified.audiobook.api.PlayerDownloadWholeBookTaskType
import org.slf4j.LoggerFactory

/**
 * A fake download task.
 */

class MockingDownloadWholeBookTask(
  private val audioBook: MockingAudioBook
) : PlayerDownloadWholeBookTaskType {

  private val log = LoggerFactory.getLogger(MockingDownloadWholeBookTask::class.java)

  override fun fetch() {
    this.audioBook.spineItems.map { item -> item.downloadTask().fetch() }
  }

  override fun cancel() {
    this.audioBook.spineItems.map { item -> item.downloadTask().cancel() }
  }

  override fun delete() {
    this.audioBook.spineItems.map { item -> item.downloadTask().delete() }
  }

  override val progress: Double
    get() = calculateProgress()

  private fun calculateProgress(): Double {
    return this.audioBook.spineItems.sumByDouble { item -> item.downloadTask().progress } / this.audioBook.spineItems.size
  }
}
