package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseError
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifestEncrypted
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestMetadata
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType

/**
 * A parser that parses metadata objects.
 */

class WebPubMetadataParser(
  private val extensions: List<ManifestParserExtensionType>,
  private val onExtensionValueProvided: (PlayerManifestExtensionValueType) -> Unit,
  onReceive: (FRParserContextType, PlayerManifestMetadata) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifestMetadata>(onReceive) {

  private var encrypted: PlayerManifestEncrypted? = null
  private lateinit var identifier: String
  private lateinit var title: String
  private val errors = mutableListOf<FRParseError>()

  override fun onCompleted(
    context: FRParserContextType
  ): FRParseResult<PlayerManifestMetadata> {
    return FRParseResult.errorsOr(this.errors) {
      FRParseResult.succeed(
        PlayerManifestMetadata(
          title = this.title,
          identifier = this.identifier,
          encrypted = this.encrypted
        )
      )
    }
  }

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val titleSchema =
      FRParserObjectFieldSchema(
        name = "title",
        parser = {
          FRValueParsers.forString { title ->
            this.title = title
          }
        }
      )

    val identifierSchema =
      FRParserObjectFieldSchema(
        name = "identifier",
        parser = {
          FRValueParsers.forString { identifier ->
            this.identifier = identifier
          }
        }
      )

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

    /*
     * Register the existing schema fields, then consult any registered extensions to
     * see if there are more.
     */

    val schemas = mutableMapOf<String, FRParserObjectFieldSchema<*>>()
    schemas[titleSchema.name] = titleSchema
    schemas[identifierSchema.name] = identifierSchema
    schemas[encryptedSchema.name] = encryptedSchema

    WebPubParserExtensions.addToSchemas(
      context = context,
      containerName = "top-level",
      extensions = this.extensions,
      schemas = schemas,
      extensionMethod = { extension ->
        extension.metadataObjectSchemas(this.onExtensionValueProvided::invoke)
      },
      onError = { error ->
        this.errors.add(error)
      }
    )
    return FRParserObjectSchema(schemas.values.toList())
  }
}
