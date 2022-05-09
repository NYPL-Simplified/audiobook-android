package org.librarysimplified.audiobook.api

import org.joda.time.Duration

/**
 * A spine item.
 */

interface PlayerSpineElementType {

  /**
   * The book to which this spine element belongs.
   */

  val book: PlayerAudioBookType

  /**
   * The index of the spine item within the spine.
   *
   * The first item in the spine, if it exists, is guaranteed to have index = 0.
   * The next spine item, if it exists, is guaranteed to be at index + 1.
   */

  val index: Int

  /**
   * The next spine item, if one exists. This is null if and only if the current spine element
   * is the last one in the book.
   */

  val next: PlayerSpineElementType?

  /**
   * The previous spine item, if one exists. This is null if and only if the current spine element
   * is the first one in the book.
   */

  val previous: PlayerSpineElementType?

  /**
   * The length of the spine item, if available.
   */

  val duration: Duration?

  /**
   * The unique identifier for the spine item.
   */

  val id: String

  /**
   * The title of the spine item.
   */

  val title: String?

  /**
   * The latest published download status for the spine item.
   */

  val downloadStatus: PlayerSpineElementDownloadStatus

  /**
   * `true` if downloading individual chapters is supported by the underlying engine.
   */

  val downloadTasksSupported: Boolean

  /**
   * The download task for the spine item.
   *
   * @see downloadTasksSupported
   */

  @Throws(UnsupportedOperationException::class)
  fun downloadTask(): PlayerDownloadTaskType
}
