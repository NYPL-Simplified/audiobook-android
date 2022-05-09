package org.librarysimplified.audiobook.api

import java.io.Closeable

/**
 * An instance of an audio book. The audio book must be closed when it is no longer needed.
 */

interface PlayerAudioBookType : Closeable {

  /**
   * `true` if [close] has been called
   */

  val isClosed: Boolean

  /**
   * A unique identifier for the book.
   */

  val id: PlayerBookID

  /**
   * True iff the underlying audio book supports streaming. That is, it's not necessary to download
   * a book part before it's possible to play that part.
   */

  val supportsStreaming: Boolean

  /**
   * True iff the underlying audio engine supports the deletion of individual chapters via
   * the PlayerDownloadTaskType interface. If this is false, local book data may only be
   * deleted via the `deleteLocalChapterData` method.
   */

  val supportsIndividualChapterDeletion: Boolean

  val spine: List<PlayerSpineElementType>

  /**
   * Create a player for the audio book. The player must be closed when it is no longer needed.
   */

  fun createPlayer(): PlayerResult<PlayerType, Exception>
}
