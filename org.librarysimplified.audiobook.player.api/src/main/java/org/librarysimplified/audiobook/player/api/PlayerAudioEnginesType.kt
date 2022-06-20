package org.librarysimplified.audiobook.player.api

/**
 * An API to find engine providers for books.
 */

interface PlayerAudioEnginesType {

  /**
   * Find the "best" provider that can handle a given request.
   */

  fun findBestFor(request: PlayerAudioEngineRequest): PlayerFactoryType?
}
