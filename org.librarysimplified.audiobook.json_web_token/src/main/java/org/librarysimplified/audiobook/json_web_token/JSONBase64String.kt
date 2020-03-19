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
    mimeVariant.decode(this.text)
  }

  /**
   * Decode the Base64URL string to a byte array.
   */

  fun decode(): ByteArray {
    return mimeVariant.decode(this.text)
  }

  companion object {

    private val mimeVariant = Base64Variants.MODIFIED_FOR_URL

    /**
     * Encode the byte array as a Base64URL string.
     */

    fun encode(data: ByteArray): JSONBase64String {
      return JSONBase64String(this.mimeVariant.encode(data))
    }
  }
}
