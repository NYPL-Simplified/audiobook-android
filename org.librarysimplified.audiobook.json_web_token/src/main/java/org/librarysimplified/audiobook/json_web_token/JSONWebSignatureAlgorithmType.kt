package org.librarysimplified.audiobook.json_web_token

/**
 * A signature algorithm.
 */

interface JSONWebSignatureAlgorithmType {

  /**
   * The name of the algorithm as a JSSE name.
   */

  val name: String

  /**
   * Sign the given data, returning the signature.
   */

  fun sign(
    data: ByteArray
  ): ByteArray
}
