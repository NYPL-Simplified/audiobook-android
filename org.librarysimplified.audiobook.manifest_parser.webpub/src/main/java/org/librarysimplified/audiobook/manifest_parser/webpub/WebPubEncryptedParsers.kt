package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRValueParserType
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar

/**
 * A parser that parses 'encrypted' objects.
 */

object WebPubEncryptedParsers {

  fun forEncrypted(
    context: FRParserContextType,
    receiver: (PlayerManifestEncrypted) -> Unit = FRValueParsers.ignoringReceiver()
  ): FRValueParserType<PlayerManifestEncrypted> {
    return WebPubScalarParsers.forMap().flatMap { keys ->
      val scheme = keys["scheme"]
      if (scheme is PlayerManifestScalar.PlayerManifestScalarString) {
        val encrypted =
          PlayerManifestEncrypted(
            scheme = scheme.text,
            values = keys.minus("scheme")
          )
        receiver.invoke(encrypted)
        FRParseResult.succeed(encrypted)
      } else {
        FRParseResult.FRParseFailed(
          listOf(context.errorOf("A 'scheme' value must be provided"))
        )
      }
    }
  }
}
