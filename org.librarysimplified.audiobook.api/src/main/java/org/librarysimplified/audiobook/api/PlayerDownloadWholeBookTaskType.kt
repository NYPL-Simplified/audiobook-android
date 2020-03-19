package org.librarysimplified.audiobook.api

/**
 * A download of an entire book in progress. If the book to which this download task refers is
 * already downloaded, the task completes instantly.
 */

interface PlayerDownloadWholeBookTaskType : PlayerDownloadTaskType {

  /**
   * Run the download task.
   */

  override fun fetch()

  /**
   * Cancel the download in progress if the download is currently running, and delete any partially
   * downloaded data. Has no effect if the download task is not currently downloading anything,
   * although implementations are permitted to broadcast events indicating the current state of the
   * download.
   */

  override fun cancel()

  /**
   * Delete the downloaded data (if any). The method has no effect if the data has not been
   * downloaded. If the download is in progress, the download is cancelled as if the `cancel`
   * method had been called.
   */

  override fun delete()

  /**
   * The current download progress in the range [0, 1]
   */

  override val progress: Double
}
