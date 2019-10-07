package org.librarysimplified.audiobook.api

import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * A unique identifier for a book. The identifier is guaranteed to be of a fixed length, and
 * consisting only of characters that are safe for use in filenames.
 */

data class PlayerBookID private constructor(val value: String) {

  companion object {
    fun transform(id: String): PlayerBookID {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = digest.digest(id.toByteArray(Charset.forName("UTF-8")))
      val sb = StringBuilder(32)
      for (b in bytes) {
        sb.append(String.format("%02x", b))
      }
      return PlayerBookID(sb.toString())
    }
  }

}
