package org.nypl.audiobook.android.api

/**
 * The download status of a particular part of a book.
 */

sealed class PlayerSpineElementDownloadStatus {

  /**
   * The part of the book has not been downloaded. If the underlying audio engine supports
   * streaming, then attempting to play this part of the book will stream it from a remote
   * server.
   */

  object PlayerSpineElementNotDownloaded : PlayerSpineElementDownloadStatus()

  /**
   * The part of the book is currently downloading.
   */

  data class PlayerSpineElementDownloading(val percent: Int) : PlayerSpineElementDownloadStatus()

  /**
   * The part of the book is completely downloaded.
   */

  object PlayerSpineElementDownloaded : PlayerSpineElementDownloadStatus()

  /**
   * Downloading this part of the book failed.
   */

  data class PlayerSpineElementDownloadFailed(
    val exception: Exception?,
    val message: String)
    : PlayerSpineElementDownloadStatus()

}
