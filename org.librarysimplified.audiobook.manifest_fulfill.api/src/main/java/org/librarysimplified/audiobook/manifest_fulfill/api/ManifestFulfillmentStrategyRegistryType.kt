package org.librarysimplified.audiobook.manifest_fulfill.api

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyProviderType

/**
 * The type of manifest fulfillment strategy registries.
 */

interface ManifestFulfillmentStrategyRegistryType {

  /**
   * Find a fulfillment strategy that implements the given interface.
   */

  fun <T : ManifestFulfillmentStrategyProviderType<*>> findStrategy(
    clazz: Class<T>
  ): T?
}
