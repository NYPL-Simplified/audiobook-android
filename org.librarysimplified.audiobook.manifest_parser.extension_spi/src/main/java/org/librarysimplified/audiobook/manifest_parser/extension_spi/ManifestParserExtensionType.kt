package org.librarysimplified.audiobook.manifest_parser.extension_spi

import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType
import org.librarysimplified.audiobook.api.PlayerVersion

/**
 * A parser extension.
 */

interface ManifestParserExtensionType {

  /**
   * The base format supported by this extension provider. The extension can only be
   * used with manifest parsers that support the same base format.
   */

  val format: String

  /**
   * The name of the extension provider.
   */

  val name: String

  /**
   * The version number of the extension provider.
   */

  val version: PlayerVersion

  /**
   * A list of object schemas that will be registered whilst parsing the top-level object.
   */

  fun topLevelObjectSchemas(
    onReceive: (PlayerManifestExtensionValueType) -> Unit
  ): List<FRParserObjectFieldSchema<out PlayerManifestExtensionValueType>>
}
