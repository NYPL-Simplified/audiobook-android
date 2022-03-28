package org.librarysimplified.audiobook.manifest_fulfill.spi

import io.reactivex.Observable
import org.librarysimplified.audiobook.api.PlayerResult
import java.io.Closeable

/**
 * The type of strategies for fulfilling manifests.
 *
 * A fulfillment strategy is responsible for fetching a manifest from a remote server, and
 * returning the raw bytes representing the manifest.
 */

interface ManifestFulfillmentStrategyType : Closeable {

  /**
   * An observable source of events published during fulfillment.
   */

  val events: Observable<ManifestFulfillmentEvent>

  /**
   * Execute the strategy, returning the raw bytes of the manifest.
   */

  fun execute(): PlayerResult<ManifestFulfilled, ManifestFulfillmentErrorType>
}
