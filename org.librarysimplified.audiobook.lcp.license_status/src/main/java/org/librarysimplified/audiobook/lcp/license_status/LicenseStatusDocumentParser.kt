package org.librarysimplified.audiobook.lcp.license_status

import one.irradia.fieldrush.api.FRAbstractParserObject
import one.irradia.fieldrush.api.FRParseResult
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import one.irradia.fieldrush.api.FRParserObjectSchema
import one.irradia.fieldrush.api.FRValueParserProviderType

/**
 * The root license status document parser.
 */

class LicenseStatusDocumentParser(
  private val valueParsers: FRValueParserProviderType,
  onReceive: (FRParserContextType, LicenseStatusDocument) -> Unit = valueParsers.ignoringReceiverWithContext()
) : FRAbstractParserObject<LicenseStatusDocument>(onReceive) {

  private var status: LicenseStatusDocument.Status =
    LicenseStatusDocument.Status.ACTIVE

  override fun onCompleted(
    context: FRParserContextType
  ): FRParseResult<LicenseStatusDocument> {
    return FRParseResult.succeed(
      LicenseStatusDocument(
        status = this.status
      )
    )
  }

  override fun schema(
    context: FRParserContextType
  ): FRParserObjectSchema {

    val statusSchema =
      FRParserObjectFieldSchema(
        name = "status",
        parser = {
          LicenseStatusValueParser { _, status ->
            this.status = status
          }
        },
        isOptional = true
      )

    return FRParserObjectSchema(fields = listOf(statusSchema))
  }
}
