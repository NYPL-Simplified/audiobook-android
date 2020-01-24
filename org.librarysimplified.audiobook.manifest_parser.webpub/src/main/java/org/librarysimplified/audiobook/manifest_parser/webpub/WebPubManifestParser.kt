package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.librarysimplified.audiobook.api.PlayerManifest
import org.librarysimplified.audiobook.api.PlayerManifestLink
import org.librarysimplified.audiobook.api.PlayerManifestMetadata

/**
 * A parser that parses manifest objects.
 */

class WebPubManifestParser(
  onReceive: (FRParserContextType, PlayerManifest) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<PlayerManifest>(onReceive) {

  private lateinit var metadata: PlayerManifestMetadata
  private val spineItems = mutableListOf<PlayerManifestLink>()
  private val links = mutableListOf<PlayerManifestLink>()

  override fun onCompleted(context: FRParserContextType): FRParseResult<PlayerManifest> {
    return FRParseResult.succeed(
      PlayerManifest(
        readingOrder = this.spineItems.toList(),
        metadata = this.metadata,
        links = this.links.toList()
      )
    )
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

    return FRParserObjectSchema(
      listOf(
        metadataSchema,
        readingOrderSchema,
        linksSchema
      )
    )
  }
}
