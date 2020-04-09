package org.librarysimplified.audiobook.manifest_fulfill.opa

import org.librarysimplified.audiobook.api.PlayerUserAgent
import org.librarysimplified.audiobook.manifest_fulfill.spi.ManifestFulfillmentStrategyParametersType

/**
 * Parameters for Overdrive Patron Authentication.
 *
 * @see "https://developer.overdrive.com/apis/patron-auth"
 */

data class OPAParameters(
  val userName: String,
  val password: OPAPassword,
  val clientKey: String,
  val clientPass: String,
  val targetURI: OPAManifestURI,
  override val userAgent: PlayerUserAgent
) : ManifestFulfillmentStrategyParametersType
