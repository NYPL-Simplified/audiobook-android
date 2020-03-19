package org.librarysimplified.audiobook.json_web_token

import com.fasterxml.jackson.core.Base64Variants
import java.io.Serializable

/**
 * A Base64URL encoded string.
 */

data class JSONBase64String(
  val text: String
) : Serializable {

  init {
    Base64Variants.MIME.decode(this.text)
  }

  /**
   * Decode the Base64URL string to a byte array.
   */

  fun decode(): ByteArray {
    return Base64Variants.MIME_NO_LINEFEEDS.decode(this.text)
  }

  companion object {

    /**
     * Encode the byte array as a Base64URL string.
     */

    fun encode(data: ByteArray): JSONBase64String {
      return JSONBase64String(Base64Variants.MIME_NO_LINEFEEDS.encode(data))
    }
  }
}
