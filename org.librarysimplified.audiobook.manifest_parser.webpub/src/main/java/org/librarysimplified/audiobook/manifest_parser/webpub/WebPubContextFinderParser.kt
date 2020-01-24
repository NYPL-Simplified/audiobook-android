package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserArrayOrSingleType
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.slf4j.LoggerFactory

/**
 * A parser that extracts the contents of a `@context` field in a given object, but ignores
 * everything else.
 */

class WebPubContextFinderParser(
  onReceive: (FRParserContextType, List<String>) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<List<String>>(onReceive) {

  private val logger =
    LoggerFactory.getLogger(WebPubContextFinderParser::class.java)
  private val contextValues =
    mutableListOf<String>()

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    return FRParserObjectSchema(
      listOf(
        FRParserObjectFieldSchema(
          name = "@context",
          parser = this::createContextValueParser,
          isOptional = false
        )
      )
    )
  }

  /**
   * Create a parser that parses a `@context` field. A `@context` field may either have
   * a scalar string value, or it might have an array value where each element is either
   * a string or an object. We can't do anything useful with the object values, but we
   * are interested in all of the strings.
   */

  private fun createContextValueParser(): FRParserArrayOrSingleType<String> {
    return FRValueParsers.forArrayOrSingle(
      forItem = {
        FRValueParsers.forScalarOrObject(
          forScalar = {
            FRValueParsers.forString { value ->
              this.logger.debug("received context value: {}", value)
              this.contextValues.add(value)
            }
          },
          forObject = {
            FRValueParsers.ignores().map { "" }
          }
        )
      }
    )
  }

  override fun onCompleted(context: FRParserContextType): FRParseResult<List<String>> =
    FRParseResult.succeed(this.contextValues.toList())
}