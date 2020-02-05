package org.librarysimplified.audiobook.json_web_token

import com.fasterxml.jackson.core.Base64Variants

/**
 * A Base64URL encoded string.
 */

data class JSONBase64String(
  val text: String
) {

  /**
   * Decode the Base64URL string to a byte array.
   */

  fun decode(): ByteArray {
    return Base64Variants.MODIFIED_FOR_URL.decode(this.text)
  }

  companion object {

    /**
     * Encode the byte array as a Base64URL string.
     */

    fun encode(data: ByteArray): JSONBase64String {
      return JSONBase64String(Base64Variants.MODIFIED_FOR_URL.encode(data))
    }
  }
}
