package org.librarysimplified.audiobook.parser.api

import java.io.Closeable

/**
 * A parser.
 */

interface ParserType<T> : Closeable {

  /**
   * Evaluate the parser.
   */

  fun parse(): ParseResult<T>
}
