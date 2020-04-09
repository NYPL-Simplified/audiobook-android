package org.librarysimplified.audiobook.manifest_fulfill.spi

/**
 * The type of providers of strategies for fulfilling manifests.
 *
 * @see [ManifestFulfillmentStrategyType]
 */

interface ManifestFulfillmentStrategyProviderType<T : ManifestFulfillmentStrategyParametersType> {

  /**
   * Create a new manifest fulfillment strategy.
   */

  fun create(
    configuration: T
  ): ManifestFulfillmentStrategyType
}
