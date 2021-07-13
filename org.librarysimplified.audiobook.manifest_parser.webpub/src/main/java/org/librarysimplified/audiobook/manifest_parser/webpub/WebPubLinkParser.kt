package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.api.FRValueParserType
import one.irradia.fieldrush.vanilla.FRValueParsers
import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLinkProperties
import org.librarysimplified.audiobook.manifest.api.PlayerManifestScalar
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.URI

/**
 * A parser that parses a link object in a WebPub manifest.
 */

class WebPubLinkParser(
  onReceive: (FRParserContextType, PlayerManifestLink) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifestLink>(onReceive) {

  private val logger =
    LoggerFactory.getLogger(WebPubLinkParser::class.java)

  private var href: String? = null
  private val relations = mutableListOf<String>()
  private var bitrate: Double? = null
  private var duration: Double? = null
  private var height: BigInteger? = null
  private var isTemplated: Boolean = false
  private val extras: MutableMap<String, PlayerManifestScalar> = mutableMapOf()
  private var properties: PlayerManifestLinkProperties = PlayerManifestLinkProperties()
  private var title: String? = null
  private var type: MIMEType? = null
  private var width: BigInteger? = null
  private val alternates: MutableList<PlayerManifestLink> = mutableListOf()

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val hrefSchema =
      FRParserObjectFieldSchema(
        name = "href",
        parser = {
          FRValueParsers.acceptingNull(
            FRValueParsers.forString { uri -> this.href = uri }
          )
        },
        isOptional = true
      )

    val templatedSchema =
      FRParserObjectFieldSchema(
        name = "templated",
        parser = {
          FRValueParsers.forBoolean { isTemplated ->
            this.isTemplated = isTemplated
          }
        },
        isOptional = true
      )

    val typeSchema =
      FRParserObjectFieldSchema(
        name = "type",
        parser = {
          FRValueParsers.forMIME { mime ->
            this.type = mime
          }
        },
        isOptional = true
      )

    val relSchema =
      FRParserObjectFieldSchema(
        name = "rel",
        parser = {
          FRValueParsers.forArrayOrSingle({
            FRValueParsers.forString { relation ->
              this.relations.add(relation)
            }
          })
        },
        isOptional = true
      )

    val titleSchema =
      FRParserObjectFieldSchema(
        name = "title",
        parser = {
          FRValueParsers.acceptingNull(
            FRValueParsers.forString { title -> this.title = title }
          )
        },
        isOptional = true
      )

    val widthSchema =
      FRParserObjectFieldSchema(
        name = "width",
        parser = {
          FRValueParsers.forInteger { width -> this.width = width }
        },
        isOptional = true
      )

    val heightSchema =
      FRParserObjectFieldSchema(
        name = "height",
        parser = {
          FRValueParsers.forInteger { height -> this.height = height }
        },
        isOptional = true
      )

    val durationSchema =
      FRParserObjectFieldSchema(
        name = "duration",
        parser = {
          FRValueParsers.forReal { duration -> this.duration = duration }
        },
        isOptional = true
      )

    val bitrateSchema =
      FRParserObjectFieldSchema(
        name = "bitrate",
        parser = {
          FRValueParsers.forReal { bitrate -> this.bitrate = bitrate }
        },
        isOptional = true
      )

    val propertiesSchema =
      FRParserObjectFieldSchema(
        name = "properties",
        parser = {
          FRValueParsers.forScalarOrObject(
            forScalar = {
              FRValueParsers.forScalarOrNull({ _ ->
                FRParseResult.succeed(PlayerManifestLinkProperties())
              })
            },
            forObject = {
              WebPubLinkPropertiesParser { _, properties ->
                this.properties = properties
              } as FRValueParserType<PlayerManifestLinkProperties?>
            }
          )
        },
        isOptional = true
      )

    val alternatesSchema =
      FRParserObjectFieldSchema(
        name = "alternates",
        parser = {
          FRValueParsers.forArrayOrSingle(
            {
              WebPubLinkParser { _, alternate ->
                this.alternates.add(alternate)
              }
            }
          )
        },
        isOptional = true
      )

    return FRParserObjectSchema(
      fields = listOf(
        alternatesSchema,
        bitrateSchema,
        durationSchema,
        heightSchema,
        hrefSchema,
        propertiesSchema,
        relSchema,
        templatedSchema,
        titleSchema,
        typeSchema,
        widthSchema
      ),
      unknownField = { _, name ->
        WebPubScalarParsers.forManifestScalar { scalar ->
          this.extras[name] = scalar
        }
      }
    )
  }

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifestLink> {
    this.extras.putAll(this.properties.extras)

    val mergedProperties =
      this.properties.copy(extras = this.extras.toMap())

    return if (this.isTemplated) {
      FRParseResult.succeed(
        PlayerManifestLink.LinkTemplated(
          alternates = this.alternates.toList(),
          bitrate = this.bitrate,
          duration = this.duration,
          height = this.height?.toInt(),
          href = this.href ?: "",
          properties = mergedProperties,
          relation = this.relations.toList(),
          title = this.title,
          type = this.type,
          width = this.width?.toInt()
        )
      )
    } else {
      try {
        FRParseResult.succeed(
          PlayerManifestLink.LinkBasic(
            alternates = this.alternates.toList(),
            bitrate = this.bitrate,
            duration = this.duration,
            height = this.height?.toInt(),
            href = this.href?.let { URI(it) },
            properties = mergedProperties,
            relation = this.relations.toList(),
            title = this.title,
            type = this.type,
            width = this.width?.toInt()
          ) as PlayerManifestLink
        )
      } catch (e: Exception) {
        this.logger.error("could not parse URI: ", e)
        FRParseResult.FRParseFailed(
          warnings = listOf(),
          errors = listOf(context.errorOf(e.message ?: e.javaClass.name, e))
        )
      }
    }
  }
}
