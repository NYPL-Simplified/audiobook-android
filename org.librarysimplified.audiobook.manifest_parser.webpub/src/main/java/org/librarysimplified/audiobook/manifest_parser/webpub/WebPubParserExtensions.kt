package org.librarysimplified.audiobook.manifest_parser.webpub

import one.irradia.fieldrush.api.FRParseError
import one.irradia.fieldrush.api.FRParserContextType
import one.irradia.fieldrush.api.FRParserObjectFieldSchema
import org.librarysimplified.audiobook.manifest.api.PlayerManifestExtensionValueType
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.slf4j.LoggerFactory

/*
 * Convenience functions to populate parsers with extensions.
 */

object WebPubParserExtensions {

  private val logger =
    LoggerFactory.getLogger(WebPubParserExtensions::class.java)

  /**
   * Add the given extension schemas to the current set of fields. Extensions will be consulted
   * using [extensionMethod] and the returned list of field schemas for each extension will be
   * added to [schemas].
   *
   * @param context The current parsing context
   * @param containerName The name of the object that will contain the extensions (for diagnostic purposes)
   * @param extensions The list of extension providers
   * @param extensionMethod A function that, given an extension provider, returns a list of field schemas
   * @param schemas The mutable map of current field schemas
   * @param onError A function that will receive parse errors
   */

  fun addToSchemas(
    context: FRParserContextType,
    containerName: String,
    extensions: List<ManifestParserExtensionType>,
    extensionMethod: (ManifestParserExtensionType) -> List<FRParserObjectFieldSchema<out PlayerManifestExtensionValueType>>,
    schemas: MutableMap<String, FRParserObjectFieldSchema<*>>,
    onError: (FRParseError) -> Unit
  ) {
    this.logger.debug("{} extensions registered", extensions.size)
    var extensionObjectsAvailable = 0
    var extensionObjectsUsed = 0
    for (extension in extensions) {
      if (extension.format != WebPub.baseFormat) {
        onError.invoke(
          FRParseError(
            extension.name,
            context.jsonStream.currentPosition,
            "The extension ${extension.name} has format ${extension.format}, which is not compatible with ${WebPub.baseFormat}",
            IllegalStateException()
          )
        )
        continue
      }

      val extensionSchemas = extensionMethod.invoke(extension)
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

        onError.invoke(
          FRParseError(
            extension.name,
            context.jsonStream.currentPosition,
            "An object schema for the '$containerName' field '${extensionSchema.name}' is already registered",
            IllegalStateException()
          )
        )
      }
    }

    this.logger.debug(
      "registered {} of {} available '$containerName' extension object schemas",
      extensionObjectsUsed,
      extensionObjectsAvailable
    )
  }
}
