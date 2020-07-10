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
 * A set of JSON Web Token claims. These claims are typically used as the payload of
 * a JSON Web Signature
 *
 * @see [JSONWebSignature]
 * @see [JOSEHeader]
 * @see "https://tools.ietf.org/html/rfc7519"
 */

data class JSONWebTokenClaims(
  val claims: Map<String, String>
) {

  /**
   * Identifies principal that issued the JWT.
   */

  val issuer: String? = this.claims["iss"]

  /**
   * Subject	Identifies the subject of the JWT.
   */

  val subject: String? = this.claims["sub"]

  /**
   * Identifies the recipients that the JWT is intended for. Each principal intended to process
   * the JWT must identify itself with a value in the audience claim. If the principal processing
   * the claim does not identify itself with a value in the aud claim when this claim is present,
   * then the JWT must be rejected.
   */

  val audience: String? = this.claims["aud"]

  /**
   * Identifies the expiration time on and after which the JWT must not be accepted for processing.
   * The value must be a NumericDate:[9] either an integer or decimal,
   * representing seconds past 1970-01-01 00:00:00Z.
   */

  val expiration: String? = this.claims["exp"]

  /**
   * Identifies the time on which the JWT will start to be accepted for processing.
   * The value must be a NumericDate.
   */

  val notBefore: String? = this.claims["nbf"]

  /**
   * Identifies the time at which the JWT was issued. The value must be a NumericDate.
   */

  val issuedAt: String? = this.claims["iat"]

  /**
   * Case sensitive unique identifier of the token even among different issuers.
   */

  val jwtId: String? = this.claims["jti"]

  /**
   * The "typ" (type) Header Parameter defined by [JSONWebSignature] and [JWE] is used by JWT applications
   * to declare the media type [IANA.MediaTypes] of this complete JWT.
   */

  val type: String? = this.claims["typ"]

  /**
   * The "cty" (content type) Header Parameter defined by [JSONWebSignature] and [JWE]
   * is used by this specification to convey structural information about
   * the JWT.
   */

  val contentType: String? = this.claims["cty"]

  companion object {

    /**
     * Serialize the given claims to a JSON object.
     */

    fun toObjectNode(
      claims: JSONWebTokenClaims
    ): ObjectNode {
      val mapper = ObjectMapper()
      val objectNode = mapper.createObjectNode()
      for (claimEntry in claims.claims) {
        objectNode.put(claimEntry.key, claimEntry.value)
      }
      return objectNode
    }

    /**
     * Encode the given claims to a Base64URL string.
     */

    fun encode(claims: JSONWebTokenClaims): JSONBase64String {
      return JSONBase64String.encode(
        JSONCanonicalization.canonicalize(
          toObjectNode(claims)
        )
      )
    }

    /**
     * Decode and parse a JOSE header from the given Base64URL string.
     */

    fun decode(
      uri: URI,
      text: JSONBase64String
    ): ParseResult<JSONWebTokenClaims> {
      return parse(uri, text.decode())
    }

    /**
     * Parse a JOSE header from the given byte array. The byte array is assumed to represent
     * UTF-8 encoded JSON text.
     */

    fun parse(
      uri: URI,
      data: ByteArray
    ): ParseResult<JSONWebTokenClaims> {
      val parser =
        FRParsers()
          .createParser(
            uri = uri,
            stream = ByteArrayInputStream(data),
            rootParser = FRValueParsers.forObjectMap(
              forKey = {
                FRValueParsers.acceptingNull(FRValueParsers.forString())
              }
            )
          )

      return when (val result = parser.parse()) {
        is FRParseResult.FRParseSucceeded -> {
          ParseResult.Success(
            warnings = listOf(),
            result = JSONWebTokenClaims(JSONUtilities.filterNotNull(result.result))
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
