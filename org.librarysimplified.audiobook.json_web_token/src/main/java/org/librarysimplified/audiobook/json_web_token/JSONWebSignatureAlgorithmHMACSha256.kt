package org.librarysimplified.audiobook.json_web_token

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A HMAC-SHA256 algorithm.
 */

class JSONWebSignatureAlgorithmHMACSha256 private constructor(
  private val secret: String
) : JSONWebSignatureAlgorithmType {

  override val name: String =
    "HmacSHA256"

  override fun sign(
    data: ByteArray
  ): ByteArray {
    val mac =
      Mac.getInstance("HmacSHA256")
    val secretKey =
      SecretKeySpec(this.secret.toByteArray(Charsets.US_ASCII), "HmacSHA256")

    mac.init(secretKey)
    return mac.doFinal(data)
  }

  companion object {

    /**
     * Create a new signature algorithm based on the given secret.
     */

    fun withSecret(secret: String): JSONWebSignatureAlgorithmType {
      return JSONWebSignatureAlgorithmHMACSha256(secret)
    }
  }
}
