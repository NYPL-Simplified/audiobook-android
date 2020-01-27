package org.librarysimplified.audiobook.feedbooks

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

/**
 * A parser for feedbooks rights values.
 */

class FeedbooksRightsParser(
  onReceive: (FRParserContextType, FeedbooksRights) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<FeedbooksRights>(onReceive) {

  private lateinit var validStart: LocalDateTime
  private lateinit var validEnd: LocalDateTime

  override fun onCompleted(context: FRParserContextType): FRParseResult<FeedbooksRights> {
    return FRParseResult.succeed(
      FeedbooksRights(
        validStart = this.validStart,
        validEnd = this.validEnd
      )
    )
  }

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val validStartSchema =
      FRParserObjectFieldSchema(
        name = "start",
        parser = {
          FRValueParsers.forTimestamp { time ->
            this.validStart = LocalDateTime(time, DateTimeZone.UTC)
          }
        })

    val validEndSchema =
      FRParserObjectFieldSchema(
        name = "end",
        parser = {
          FRValueParsers.forTimestamp { time ->
            this.validEnd = LocalDateTime(time, DateTimeZone.UTC)
          }
        })

    return FRParserObjectSchema(listOf(validStartSchema, validEndSchema))
  }
}