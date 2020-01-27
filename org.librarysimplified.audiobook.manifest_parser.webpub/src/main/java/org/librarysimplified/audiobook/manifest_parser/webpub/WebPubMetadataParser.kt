package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.manifest.api.PlayerManifestMetadata

/**
 * A parser that parses metadata objects.
 */

class WebPubMetadataParser(
  onReceive: (FRParserContextType, PlayerManifestMetadata) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifestMetadata>(onReceive) {

  private var encrypted: PlayerManifestEncrypted? = null
  private lateinit var identifier: String
  private lateinit var title: String

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifestMetadata> {
    return FRParseResult.succeed(
      PlayerManifestMetadata(
        title = this.title,
        identifier = this.identifier,
        encrypted = this.encrypted
      )
    )
  }

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val titleSchema =
      FRParserObjectFieldSchema(
        name = "title",
        parser = {
          FRValueParsers.forString { title -> this.title = title }
        }
      )

    val identifierSchema =
      FRParserObjectFieldSchema(
        name = "identifier",
        parser = {
          FRValueParsers.forString { identifier -> this.identifier = identifier }
        }
      )

    val encryptedSchema =
      FRParserObjectFieldSchema(
        name = "encrypted",
        parser = {
          WebPubEncryptedParsers.forEncrypted(context)
        },
        isOptional = true
      )

    return FRParserObjectSchema(
      listOf(
        encryptedSchema,
        identifierSchema,
        titleSchema
      )
    )
  }
}
