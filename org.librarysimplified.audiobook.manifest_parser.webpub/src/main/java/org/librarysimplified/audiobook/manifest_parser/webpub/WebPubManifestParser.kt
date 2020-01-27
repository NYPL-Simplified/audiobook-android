package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseError
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.librarysimplified.audiobook.manifest.api.PlayerManifestMetadata
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.slf4j.LoggerFactory

/**
 * A parser that parses manifest objects.
 */

class WebPubManifestParser(
  private val extensions: List<ManifestParserExtensionType>,
  onReceive: (FRParserContextType, PlayerManifest) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifest>(onReceive) {

  private val logger =
    LoggerFactory.getLogger(WebPubManifestParser::class.java)

  private lateinit var metadata: PlayerManifestMetadata
  private val spineItems = mutableListOf<PlayerManifestLink>()
  private val links = mutableListOf<PlayerManifestLink>()
  private val extensionValues = mutableListOf<PlayerManifestExtensionValueType>()
  private val errors = mutableListOf<FRParseError>()

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifest> {
    return FRParseResult.errorsOr(this.errors) {
      FRParseResult.succeed(
        PlayerManifest(
          readingOrder = this.spineItems.toList(),
          metadata = this.metadata,
          links = this.links.toList(),
          extensions = this.extensionValues.toList()
        )
      )
    }
  }

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val metadataSchema =
      FRParserObjectFieldSchema(
        name = "metadata",
        parser = {
          WebPubMetadataParser { _, metadata ->
            this.metadata = metadata
          }
        }
      )

    val readingOrderSchema =
      FRParserObjectFieldSchema(
        name = "readingOrder",
        parser = {
          FRValueParsers.forArrayMonomorphic(
            forEach = {
              WebPubLinkParser()
            },
            receiver = { spineItems ->
              this.spineItems.addAll(spineItems)
            })
        }
      )

    val linksSchema =
      FRParserObjectFieldSchema(
        name = "links",
        parser = {
          FRValueParsers.forArrayMonomorphic(
            forEach = {
              WebPubLinkParser()
            },
            receiver = { links ->
              this.links.addAll(links)
            })
        },
        isOptional = true
      )

    return finishSchema(
      metadataSchema,
      readingOrderSchema,
      linksSchema,
      context
    )
  }

  private fun finishSchema(
    metadataSchema: FRParserObjectFieldSchema<PlayerManifestMetadata>,
    readingOrderSchema: FRParserObjectFieldSchema<List<PlayerManifestLink>>,
    linksSchema: FRParserObjectFieldSchema<List<PlayerManifestLink>>,
    context: FRParserContextType
  ): FRParserObjectSchema {

    /*
     * Register the existing schema fields, then consult any registered extensions to
     * see if there are more.
     */

    val schemas = mutableMapOf<String, FRParserObjectFieldSchema<*>>()
    schemas[metadataSchema.name] = metadataSchema
    schemas[readingOrderSchema.name] = readingOrderSchema
    schemas[linksSchema.name] = linksSchema

    this.logger.debug("{} extensions registered", this.extensions.size)
    var extensionObjectsAvailable = 0
    var extensionObjectsUsed = 0
    for (extension in this.extensions) {
      if (extension.format != WebPub.baseFormat) {
        this.errors.add(
          FRParseError(
            extension.name,
            context.jsonStream.currentPosition,
            "The extension ${extension.name} has format ${extension.format}, which is not compatible with ${WebPub.baseFormat}",
            IllegalStateException()
          )
        )
        continue
      }

      val extensionSchemas =
        extension.topLevelObjectSchemas(onReceive = {
          extensionValue -> this.extensionValues.add(extensionValue)
        })

      extensionObjectsAvailable += extensionSchemas.size

      this.logger.debug(
        "extension provider {} {} returned {} object schemas",
        extension.name,
        extension.version,
        extensionSchemas.size
      )

      for (extensionSchema in extensionSchemas) {
        if (!schemas.containsKey(extensionSchema.name)) {
          schemas[extensionSchema.name] = extensionSchema
          extensionObjectsUsed += 1
          continue
        }

        /*
         * It is always a bug to register two object schemas with the same name.
         */

        this.errors.add(
          FRParseError(
            extension.name,
            context.jsonStream.currentPosition,
            "An object schema for the top-level field '${extensionSchema.name}' is already registered",
            IllegalStateException()
          )
        )
      }
    }

    this.logger.debug(
      "registered {} of {} available top-level extension object schemas",
      extensionObjectsUsed,
      extensionObjectsAvailable
    )
    return FRParserObjectSchema(schemas.values.toList())
  }
}
