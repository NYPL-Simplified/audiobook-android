package org.nypl.audiobook.android.api

/**
 * An API to find engine providers for books.
 */

interface PlayerAudioEnginesType {

  /**
   * Find all providers that can handle the given request.
   */

  fun findAllFor(request: PlayerAudioEngineRequest): List<PlayerEngineAndBookProvider>

  /**
   * Find the "best" provider that can handle a given request.
   *
   * The default implementation of this method finds all providers that can handle a given book,
   * sorts the list of providers by their version number, and picks whichever provider has the
   * highest version number.
   */

  fun findBestFor(request: PlayerAudioEngineRequest): PlayerEngineAndBookProvider? {
    return findAllFor(request).sortedBy { pair -> pair.engineProvider.version() }.lastOrNull()
  }
}