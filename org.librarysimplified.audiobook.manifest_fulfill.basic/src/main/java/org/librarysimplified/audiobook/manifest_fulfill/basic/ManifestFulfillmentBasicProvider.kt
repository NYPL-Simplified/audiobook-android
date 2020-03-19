package org.librarysimplified.audiobook.manifest_fulfill.basic

import okhttp3.OkHttpClient
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyType

/**
 * A provider of basic-auth manifest fulfillment strategies.
 *
 * Note: This class _MUST_ have a public no-arg constructor in order to work with [java.util.ServiceLoader].
 */

class ManifestFulfillmentBasicProvider(
  private val client: OkHttpClient
) : ManifestFulfillmentBasicType {

  constructor() : this(OkHttpClient())

  override fun create(
    configuration: ManifestFulfillmentBasicParameters
  ): ManifestFulfillmentStrategyType {
    return ManifestFulfillmentBasic(
      client = this.client,
      configuration = configuration
    )
  }
}
