package org.librarysimplified.audiobook.manifest_fulfill.api

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyProviderType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * The default, [ServiceLoader]-based manifest fulfillment strategy registry.
 */

object ManifestFulfillmentStrategies : ManifestFulfillmentStrategyRegistryType {

  private val logger =
    LoggerFactory.getLogger(ManifestFulfillmentStrategies::class.java)

  override fun <T : ManifestFulfillmentStrategyProviderType<*>> findStrategy(
    clazz: Class<T>
  ): T? {
    val candidates = ServiceLoader.load(clazz).toList()
    if (candidates.isEmpty()) {
      this.logger.error("no fulfillment strategies available of type {}", clazz)
      return null
    }

    for (candidate in candidates) {
      this.logger.debug("available fulfillment strategy: {}", candidate)
    }

    return candidates.firstOrNull()
  }
}
