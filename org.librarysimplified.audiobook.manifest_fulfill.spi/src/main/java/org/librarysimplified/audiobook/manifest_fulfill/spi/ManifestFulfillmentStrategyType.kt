package org.librarysimplified.audiobook.manifest_fulfill.spi

import org.librarysimplified.audiobook.api.PlayerResult

/**
 * The type of strategies for fulfilling manifests.
 *
 * A fulfillment strategy is responsible for fetching a manifest from a remote server, and
 * returning the raw bytes representing the manifest.
 */

interface ManifestFulfillmentStrategyType {

  /**
   * Execute the strategy, returning the raw bytes of the manifest.
   */

  fun execute(): PlayerResult<ByteArray, ManifestFulfillmentError>
}
