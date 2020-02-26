package org.librarysimplified.audiobook.manifest_fulfill.basic

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyProviderType

/**
 * The type of providers that download manifests directly using HTTP Basic authentication.
 */

interface ManifestFulfillmentBasicType :
  ManifestFulfillmentStrategyProviderType<ManifestFulfillmentBasicParameters>
