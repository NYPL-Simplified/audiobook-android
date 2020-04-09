package org.librarysimplified.audiobook.license_check.spi

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
    parameters: SingleLicenseCheckParameters
  ): SingleLicenseCheckType
}
