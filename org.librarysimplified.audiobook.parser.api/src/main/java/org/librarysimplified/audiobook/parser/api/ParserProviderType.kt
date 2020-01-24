package org.librarysimplified.audiobook.parser.api

import java.net.URI

/**
 * A generic provider of parsers.
 */

interface ParserProviderType<S, T> {

  /**
   * Create a new parser using the given input, and the given URI for diagnostic purposes.
   */

  fun createParser(
    uri: URI,
    streams: () -> S,
    warningsAsErrors: Boolean = false
  ): ParserType<T>

}
