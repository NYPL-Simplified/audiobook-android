package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRValueParserType
import org.librarysimplified.audiobook.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.api.PlayerManifestScalar

/**
 * A parser that parses 'encrypted' objects.
 */

object WebPubEncryptedParsers {

  fun forEncrypted(context: FRParserContextType): FRValueParserType<PlayerManifestEncrypted> {
    return WebPubScalarParsers.forMap().flatMap { keys ->
      val scheme = keys["scheme"]
      if (scheme is PlayerManifestScalar.PlayerManifestScalarString) {
        FRParseResult.succeed(
          PlayerManifestEncrypted(
            scheme = scheme.text,
            values = keys.minus("scheme")
          )
        )
      } else {
        FRParseResult.FRParseFailed(
          listOf(context.errorOf("A 'scheme' value must be provided"))
        )
      }
    }
  }
}