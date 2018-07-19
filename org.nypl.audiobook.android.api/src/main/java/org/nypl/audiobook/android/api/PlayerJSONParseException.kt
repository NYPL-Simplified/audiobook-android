package org.nypl.audiobook.android.api

import java.io.IOException

/**
 * The type of errors raised during attempts to parse JSON data.
 */

class PlayerJSONParseException : IOException {

  /**
   * Construct an exception with no message or cause.
   */

  constructor() : super() {}

  /**
   * Construct an exception.
   *
   * @param message The message
   */

  constructor(
    message: String) : super(message) {
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  constructor(
    message: String,
    cause: Throwable) : super(message, cause) {
  }

  /**
   * Construct an exception
   *
   * @param cause The case
   */

  constructor(
    cause: Throwable) : super(cause) {
  }

  companion object {
    private val serialVersionUID = 1L
  }
}