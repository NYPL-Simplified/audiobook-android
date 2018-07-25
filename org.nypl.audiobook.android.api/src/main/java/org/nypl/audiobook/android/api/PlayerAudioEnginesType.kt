package org.nypl.audiobook.android.api

/**
 * An API to find engine providers for books.
 */

interface PlayerAudioEnginesType {

  /**
   * Find all providers that can handle a given book.
   */

  fun findAllFor(
    manifest: PlayerManifest,
    filter: (PlayerAudioEngineProviderType) -> Boolean): List<PlayerEngineAndBookProvider>

  /**
   * Find all providers that can handle a given book.
   */

  fun findAllFor(
    manifest: PlayerManifest): List<PlayerEngineAndBookProvider> {
    return this.findAllFor(manifest, filter = { e -> true })
  }

  /**
   * Find the "best" provider that can handle a given book.
   *
   * The default implementation of this method finds all providers that can handle a given book,
   * sorts the list of providers by their version number, and picks whichever provider has the
   * highest version number.
   */

  fun findBestFor(
    manifest: PlayerManifest,
    filter: (PlayerAudioEngineProviderType) -> Boolean): PlayerEngineAndBookProvider? {
    return findAllFor(manifest, filter).sortedBy { pair -> pair.engineProvider.version() }.lastOrNull()
  }

}