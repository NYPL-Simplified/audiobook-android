package org.librarysimplified.audiobook.json_web_token

/**
 * A JSON Web Signature.
 *
 * @see "https://tools.ietf.org/html/rfc7515"
 */

data class JSONWebSignature(
  val header: JOSEHeader,
  val payload: ByteArray,
  val signature: JSONBase64String
) {

  /**
   * Verify that the signature can be verified against the header and payload.
   *
   * @return `true` iff the signature is valid
   */

  fun verify(
    algorithm: JSONWebSignatureAlgorithmType
  ): Boolean {
    val headerText = JOSEHeader.encode(header)
    val data = headerText.text + "." + JSONBase64String.encode(payload).text
    val newSignature = algorithm.sign(data.toByteArray())
    val oldSignature = this.signature.decode()
    return newSignature.contentEquals(oldSignature)
  }

  companion object {

    fun create(
      algorithm: JSONWebSignatureAlgorithmType,
      header: JOSEHeader,
      payload: JSONWebTokenClaims
    ): JSONWebSignature {
      return this.create(
        algorithm = algorithm,
        header = header,
        payloadText = JSONWebTokenClaims.encode(payload)
      )
    }

    fun create(
      algorithm: JSONWebSignatureAlgorithmType,
      header: JOSEHeader,
      payloadText: JSONBase64String
    ): JSONWebSignature {
      val headerText = JOSEHeader.encode(header)
      val data = headerText.text + "." + payloadText.text
      val signature = algorithm.sign(data.toByteArray())
      return JSONWebSignature(
        header = header,
        payload = payloadText.decode(),
        signature = JSONBase64String.encode(signature)
      )
    }
  }
}
