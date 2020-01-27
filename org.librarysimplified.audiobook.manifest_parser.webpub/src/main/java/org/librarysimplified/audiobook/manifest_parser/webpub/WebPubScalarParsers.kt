package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserObjectMapType
import one.irradia.fieldrush.api.FRValueParserType
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarBoolean
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarNumber.PlayerManifestScalarInteger
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarNumber.PlayerManifestScalarReal
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar.PlayerManifestScalarString

/**
 * A parser that parses objects consisting of string keys with scalar values.
 */

object WebPubScalarParsers {

  private fun textToScalar(
    text: String
  ): FRParseResult<PlayerManifestScalar> {
    return FRParseResult.succeed(
      when (val double = text.toDoubleOrNull()) {
        null ->
          when (val integer = text.toIntOrNull()) {
            null ->
              when (text) {
                "true" ->
                  PlayerManifestScalarBoolean(true)
                "false" ->
                  PlayerManifestScalarBoolean(false)
                else ->
                  PlayerManifestScalarString(text)
              }
            else ->
              PlayerManifestScalarInteger(integer)
          }
        else ->
          PlayerManifestScalarReal(double)
      }
    )
  }

  fun forManifestScalar(
    receiver: (PlayerManifestScalar) -> Unit = FRValueParsers.ignoringReceiver()
  ): FRValueParserType<PlayerManifestScalar> {
    return FRValueParsers.forScalar(
      validator = ::textToScalar,
      receiver = receiver
    )
  }

  fun forMap(
    receiver: (Map<String, PlayerManifestScalar>) -> Unit = FRValueParsers.ignoringReceiver()
  ): FRParserObjectMapType<PlayerManifestScalar> {
    return FRValueParsers.forObjectMap(
      forKey = { forManifestScalar() },
      receiver = receiver
    )
  }
}
