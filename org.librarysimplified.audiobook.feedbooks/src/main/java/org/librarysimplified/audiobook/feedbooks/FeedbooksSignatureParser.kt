package org.librarysimplified.audiobook.feedbooks

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.vanilla.FRValueParsers

/**
 * A parser for feedbooks signature values.
 */

class FeedbooksSignatureParser(
  onReceive: (FRParserContextType, FeedbooksSignature) -> Unit = FRValueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<FeedbooksSignature>(onReceive) {

  private lateinit var value: String
  private lateinit var issuer: String
  private lateinit var algorithm: String

  override fun onCompleted(context: FRParserContextType): FRParseResult<FeedbooksSignature> {
    return FRParseResult.succeed(
      FeedbooksSignature(
        algorithm = this.algorithm,
        issuer = this.issuer,
        value = this.value
      )
    )
  }

  override fun schema(context: FRParserContextType): FRParserObjectSchema {
    val algoSchema =
      FRParserObjectFieldSchema(
        name = "algorithm",
        parser = {
          FRValueParsers.forString { algo ->
            this.algorithm = algo
          }
        })

    val issuerSchema =
      FRParserObjectFieldSchema(
        name = "issuer",
        parser = {
          FRValueParsers.forString { name ->
            this.issuer = name
          }
        })

    val valueSchema =
      FRParserObjectFieldSchema(
        name = "value",
        parser = {
          FRValueParsers.forString { name ->
            this.value = name
          }
        })

    return FRParserObjectSchema(listOf(algoSchema, issuerSchema, valueSchema))
  }
}