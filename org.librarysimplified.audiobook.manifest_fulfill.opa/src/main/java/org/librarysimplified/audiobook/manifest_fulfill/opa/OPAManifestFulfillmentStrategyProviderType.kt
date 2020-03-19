package org.librarysimplified.audiobook.manifest_fulfill.opa

import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyProviderType

/**
 * The type of manifest fulfillment strategy providers that use Overdrive Patron Authentication.
 *
 * @see "https://developer.overdrive.com/apis/patron-auth"
 */

interface OPAManifestFulfillmentStrategyProviderType :
  ManifestFulfillmentStrategyProviderType<OPAParameters>
