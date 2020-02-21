package org.librarysimplified.audiobook.manifest_fulfill.api

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyProviderType
import java.util.ServiceLoader

/**
 * The default, [ServiceLoader]-based manifest fulfillment strategy registry.
 */

object ManifestFulfillmentStrategies : ManifestFulfillmentStrategyRegistryType {

  override fun <T : ManifestFulfillmentStrategyProviderType<*>> findStrategy(
    clazz: Class<T>
  ): T? {
    return ServiceLoader.load(ManifestFulfillmentStrategyProviderType::class.java)
      .toList()
      .find { provider -> provider.javaClass == clazz }
      as T?
  }
}
