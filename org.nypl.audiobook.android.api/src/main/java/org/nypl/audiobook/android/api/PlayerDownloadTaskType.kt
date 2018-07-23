package org.nypl.audiobook.android.api

/**
 * A download in progress. If the part of the book to which this download task refers is already
 * downloaded, the task completes instantly.
 */

interface PlayerDownloadTaskType {

  /**
   * Run the download task.
   */

  fun fetch()

  /**
   * Delete the downloaded data (if any). The method has no effect if the data has not been
   * downloaded. If the download is in progress, this method cancels it.
   */

  fun delete()

  /**
   * The current download progress in the range [0, 1]
   */

  val progress: Double

  /**
   * The unique ID of the download
   */

  val id: String

}