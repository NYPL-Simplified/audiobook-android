package org.librarysimplified.audiobook.license_check.spi

import org.librarysimplified.audiobook.manifest.api.PlayerManifest

/**
 * A provider of license checks.
 */

interface SingleLicenseCheckProviderType {

  /**
   * The name of the check provider.
   */

  val name: String

  /**
   * Create a new single license check, operating on the given manifest and
   * publishing events to the given event receiver.
   */

  fun createLicenseCheck(
    manifest: PlayerManifest,
    onStatusChanged: (SingleLicenseCheckStatus) -> Unit
  ): SingleLicenseCheckType

}
