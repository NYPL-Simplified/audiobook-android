package org.nypl.audiobook.android.api

import com.google.common.util.concurrent.ListenableFuture

/**
 * A provider of downloads.
 *
 * This interface is provided in order to assist applications in integrating the AudioBook API
 * into any existing download systems they may have. Unfortunately, some implementations of the
 * AudioBook API do not allow for a custom download provider to be used. The provider will be
 * used in any backend that requires one.
 */

interface PlayerDownloadProviderType {

  /**
   * Begin a download of the specified request as soon as possible. Implementors should provide
   * progress updates to the callback given in the request.
   *
   * An implementation of this method must return a future that returns success if and only if
   * data was downloaded from the given URI and has been successfully written to the specified
   * output file. Implementations are permitted to produce futures that raise exceptions for any
   * error condition.
   *
   * Implementations are required to ensure that cancelling the returned future will cancel the
   * download in progress.
   *
   * @return A future representing the download in progress. The future returns `Unit` on
   *         success, or an exception indicating what failed.
   */

  fun download(request: PlayerDownloadRequest): ListenableFuture<Unit>

}