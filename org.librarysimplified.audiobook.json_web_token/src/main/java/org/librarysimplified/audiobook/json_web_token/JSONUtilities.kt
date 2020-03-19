package org.librarysimplified.audiobook.json_web_token

import one.irradia.fieldrush.api.FRParseError
import org.librarysimplified.audiobook.parser.api.ParseError

/**
 * Utility functions to aid the implementation of other classes in this package.
 */

internal object JSONUtilities {

  /**
   * Convert a fieldrush parse error to one of our parse errors.
   */

  internal fun toParseError(
    error: FRParseError
  ): ParseError {
    return ParseError(
      source = error.position.source,
      message = error.message,
      line = error.position.line,
      column = error.position.column,
      exception = error.exception
    )
  }

  /**
   * Return a map with all null values removed (ignoring keys).
   */

  internal fun filterNotNull(
    values: Map<String, String?>
  ): Map<String, String> {
    val copy = mutableMapOf<String, String>()
    for (entry in values) {
      val value = entry.value
      if (value != null) {
        copy[entry.key] = value
      }
    }
    return copy.toMap()
  }
}
