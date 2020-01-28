package org.librarysimplified.audiobook.parser.api

import java.net.URI

/**
 * A generic provider of parsers.
 */

interface ParserProviderType<S, E, T> {

  /**
   * Create a new parser using the given input, and the given URI for diagnostic purposes.
   */

  fun createParser(
    uri: URI,
    input: S,
    extensions: List<E> = listOf(),
    warningsAsErrors: Boolean = false
  ): ParserType<T>

}
