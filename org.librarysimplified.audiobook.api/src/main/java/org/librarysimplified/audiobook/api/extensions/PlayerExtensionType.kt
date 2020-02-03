package org.librarysimplified.audiobook.api.extensions

import com.google.common.util.concurrent.FluentFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import java.util.concurrent.ExecutorService

/**
 * The type of player extensions.
 *
 * Extensions are given the opportunity to replace specific parts of the functionality of existing
 * players. This is typically required by, for example, publishers requiring special types of
 * authentication to download chapters of books.
 */

interface PlayerExtensionType {

  /**
   * The name of the extension. Conventionally, this is the fully-qualified name of the
   * extension class.
   */

  val name: String

  /**
   * Called when a chapter is about to be downloaded.
   *
   * Implementations should return a future that returns a download substitution. If no
   * replacement of the existing player functionality is required, implementations of this
   * method are permitted to return `null`.
   *
   * @param statusExecutor An executor used for publishing status updates
   * @param downloadProvider The download provider being used
   * @param link The chapter link
   */

  fun onDownloadLink(
    statusExecutor: ExecutorService,
    downloadProvider: PlayerDownloadProviderType,
    link: PlayerManifestLink
  ): FluentFuture<PlayerXDownloadSubstitution>?
}
