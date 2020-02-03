package org.librarysimplified.audiobook.api.extensions

import java.net.URI

/**
 * The type of download substitutions.
 */

sealed class PlayerXDownloadSubstitution {

  /**
   * Download the given URI instead of whichever URI would originally have been retrieved.
   */

  data class DownloadURI(
    val uri: URI
  ): PlayerXDownloadSubstitution()
}
