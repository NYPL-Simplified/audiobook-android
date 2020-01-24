package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerManifestLink
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

  private lateinit var href: String
  private val relations = mutableListOf<String>()
  private var bitrate: Double? = null
  private var duration: Double? = null
  private var height: BigInteger? = null
  private var isTemplated: Boolean = false
  private var title: String? = null
  private var type: MIMEType? = null
  private var width: BigInteger? = null

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val hrefSchema =
      FRParserObjectFieldSchema(
        name = "href",
        parser = {
          FRValueParsers.forString { uri -> this.href = uri }
        })

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
          FRValueParsers.forString { title -> this.title = title }
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

    return FRParserObjectSchema(
      listOf(
        bitrateSchema,
        durationSchema,
        heightSchema,
        hrefSchema,
        relSchema,
        templatedSchema,
        titleSchema,
        typeSchema,
        widthSchema
      )
    )
  }

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifestLink> {
    return if (this.isTemplated) {
      FRParseResult.succeed(
        PlayerManifestLink.LinkTemplated(
          href = this.href,
          type = this.type,
          relation = this.relations.toList(),
          title = this.title,
          height = this.height?.toInt(),
          width = this.width?.toInt(),
          duration = this.duration,
          bitrate = this.bitrate
        )
      )
    } else {
      try {
        FRParseResult.succeed(
          PlayerManifestLink.LinkBasic(
            href = URI(href),
            type = this.type,
            relation = this.relations.toList(),
            title = this.title,
            height = this.height?.toInt(),
            width = this.width?.toInt(),
            duration = this.duration,
            bitrate = this.bitrate
          ) as PlayerManifestLink
        )
      } catch (e: Exception) {
        this.logger.error("could not parse URI: ", e)
        FRParseResult.FRParseFailed<PlayerManifestLink>(
          listOf(context.errorOf(e.message ?: e.javaClass.name, e))
        )
      }
    }
  }
}