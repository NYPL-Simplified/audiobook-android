package org.librarysimplified.audiobook.json_web_token

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.vanilla.FRParsers
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.json_canon.JSONCanonicalization
import org.librarysimplified.audiobook.parser.api.ParseResult
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * JSON Object Signing and Encryption header.
 *
 * @see "https://tools.ietf.org/html/rfc7515"
 */

data class JOSEHeader(
  val headers: Map<String, String>
) {

  /**
   * The "alg" (algorithm) Header Parameter identifies the cryptographic
   * algorithm used to secure the JWS.
   */

  val algorithm: String? = this.headers["alg"]

  companion object {

    /**
     * Serialize the given header to a JSON object.
     */

    fun toObjectNode(
      header: JOSEHeader
    ): ObjectNode {
      val mapper = ObjectMapper()
      val objectNode = mapper.createObjectNode()
      for (headerEntry in header.headers) {
        objectNode.put(headerEntry.key, headerEntry.value)
      }
      return objectNode
    }

    /**
     * Encode the given header to a Base64URL string.
     */

    fun encode(header: JOSEHeader): JSONBase64String {
      return JSONBase64String.encode(
        JSONCanonicalization.canonicalize(
          toObjectNode(header)
        )
      )
    }

    /**
     * Decode and parse a JOSE header from the given Base64URL string.
     */

    fun decode(
      uri: URI,
      text: JSONBase64String
    ): ParseResult<JOSEHeader> {
      return parse(uri, text.decode())
    }

    /**
     * Parse a JOSE header from the given byte array. The byte array is assumed to represent
     * UTF-8 encoded JSON text.
     */

    fun parse(
      uri: URI,
      data: ByteArray
    ): ParseResult<JOSEHeader> {
      val parser =
        FRParsers()
          .createParser(
            uri = uri,
            stream = ByteArrayInputStream(data),
            rootParser = FRValueParsers.forObjectMap(forKey = {
              FRValueParsers.acceptingNull(FRValueParsers.forString())
            })
          )

      return when (val result = parser.parse()) {
        is FRParseResult.FRParseSucceeded -> {
          ParseResult.Success(
            warnings = listOf(),
            result = JOSEHeader(JSONUtilities.filterNotNull(result.result))
          )
        }
        is FRParseResult.FRParseFailed -> {
          return ParseResult.Failure(
            warnings = listOf(),
            errors = result.errors.map { error -> JSONUtilities.toParseError(error) },
            result = null
          )
        }
      }
    }
  }
}
