package org.librarysimplified.audiobook.player.api


interface PlayerDownloadManagerType {

  /**
   * Begin a download of the specified request as soon as possible. Implementors should provide
   * progress updates to the callback given in the request.
   *
   * An implementation of this method must return a future that returns success if and only if
   * data was downloaded from the given URI and has been successfully written to the specified
   * output file. Implementations are permitted to produce futures that raise exceptions for any
   * error condition.
   *
   * Implementations are required to ensure that cancelling the coroutine will cancel the
   * download in progress.
   */

  suspend fun download(
    request: PlayerDownloadRequest
  )
}
