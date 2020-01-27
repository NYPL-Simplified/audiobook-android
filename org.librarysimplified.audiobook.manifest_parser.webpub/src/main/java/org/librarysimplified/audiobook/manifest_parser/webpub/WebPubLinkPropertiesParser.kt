package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLinkProperties
import org.slf4j.LoggerFactory

/**
 * A parser that parses extra properties for a link object in a WebPub manifest.
 */

class WebPubLinkPropertiesParser(
  onReceive: (FRParserContextType, PlayerManifestLinkProperties) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifestLinkProperties>(onReceive) {

  private var encrypted: PlayerManifestEncrypted? = null

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val encryptedSchema =
      FRParserObjectFieldSchema(
        name = "encrypted",
        parser = {
          WebPubEncryptedParsers.forEncrypted(context) { encrypted ->
            this.encrypted = encrypted
          }
        },
        isOptional = true
      )

    return FRParserObjectSchema(
      listOf(
        encryptedSchema
      )
    )
  }

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifestLinkProperties> {
    return FRParseResult.succeed(
      PlayerManifestLinkProperties(
        encrypted = this.encrypted
      )
    )
  }
}