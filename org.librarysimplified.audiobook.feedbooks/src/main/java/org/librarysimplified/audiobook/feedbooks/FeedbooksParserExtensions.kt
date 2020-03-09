package org.librarysimplified.audiobook.feedbooks

import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import org.librarysimplified.audiobook.api.PlayerVersion
import org.librarysimplified.audiobook.api.PlayerVersions
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.manifest_parser.webpub.WebPub

/**
 * Parser extensions for Feedbooks audio books.
 */

class FeedbooksParserExtensions : ManifestParserExtensionType {

  override val format: String =
    WebPub.baseFormat

  override val name: String =
    "https://www.feedbooks.com"

  override val version: PlayerVersion =
    PlayerVersions.ofPropertiesClassOrNull(
      clazz = FeedbooksParserExtensions::class.java,
      path = "/org/librarysimplified/audiobook/feedbooks/provider.properties"
    ) ?: PlayerVersion(0, 0, 0)

  override fun topLevelObjectSchemas(
    onReceive: (PlayerManifestExtensionValueType) -> Unit
  ): List<FRParserObjectFieldSchema<out PlayerManifestExtensionValueType>> {
    return listOf()
  }

  override fun metadataObjectSchemas(
    onReceive: (PlayerManifestExtensionValueType) -> Unit
  ): List<FRParserObjectFieldSchema<out PlayerManifestExtensionValueType>> {

    val signatureSchema =
      FRParserObjectFieldSchema(
        name = "http://www.feedbooks.com/audiobooks/signature",
        parser = {
          FeedbooksSignatureParser { _, signature ->
            onReceive.invoke(signature)
          }
        },
        isOptional = true
      )

    val rightsSchema =
      FRParserObjectFieldSchema(
        name = "http://www.feedbooks.com/audiobooks/rights",
        parser = {
          FeedbooksRightsParser { _, signature ->
            onReceive.invoke(signature)
          }
        },
        isOptional = true
      )

    return listOf(signatureSchema, rightsSchema)
  }
}
