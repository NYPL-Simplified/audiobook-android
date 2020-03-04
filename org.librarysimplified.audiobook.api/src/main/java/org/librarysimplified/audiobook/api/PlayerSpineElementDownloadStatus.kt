package org.librarysimplified.audiobook.api

/**
 * The download status of a particular part of a book.
 */

sealed class PlayerSpineElementDownloadStatus {

  /**
   * The spine element to which this download status refers.
   */

  abstract val spineElement: PlayerSpineElementType

  /**
   * The part of the book has not been downloaded. If the underlying audio engine supports
   * streaming, then attempting to play this part of the book will stream it from a remote
   * server.
   */

  data class PlayerSpineElementNotDownloaded(
    override val spineElement: PlayerSpineElementType
  ) : PlayerSpineElementDownloadStatus()

  /**
   * The part of the book is currently downloading.
   */

  data class PlayerSpineElementDownloading(
    override val spineElement: PlayerSpineElementType,
    val percent: Int
  ) : PlayerSpineElementDownloadStatus()

  /**
   * The part of the book is completely downloaded.
   */

  data class PlayerSpineElementDownloaded(
    override val spineElement: PlayerSpineElementType
  ) : PlayerSpineElementDownloadStatus()

  /**
   * Downloading this part of the book failed.
   */

  data class PlayerSpineElementDownloadFailed(
    override val spineElement: PlayerSpineElementType,
    val exception: Exception?,
    val message: String
  ) : PlayerSpineElementDownloadStatus()

  /**
   * Downloading this part of the book failed due to an (apparently) expired link. The download
   * will likely succeed if the manifest is reloaded containing a fresh set of links.
   */

  data class PlayerSpineElementDownloadExpired(
    override val spineElement: PlayerSpineElementType,
    val exception: Exception?,
    val message: String
  ) : PlayerSpineElementDownloadStatus()
}
