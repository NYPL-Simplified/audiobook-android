package org.librarysimplified.audiobook.manifest_fulfill.spi

import org.librarysimplified.audiobook.api.PlayerUserAgent

/**
 * The base type of manifest fulfillment strategy parameters.
 */

interface ManifestFulfillmentStrategyParametersType {

  /**
   * The user agent used to make various HTTP requests.
   */

  val userAgent: PlayerUserAgent
}
