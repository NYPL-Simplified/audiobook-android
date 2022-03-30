package org.librarysimplified.audiobook.license_check.api

import io.reactivex.Observable
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import java.io.Closeable

/**
 * A license check.
 */

interface LicenseCheckType : Closeable {

  /**
   * An observable stream of status events representing the license check in progress.
   */

  val events: Observable<SingleLicenseCheckStatus>

  /**
   * Execute the license check.
   */

  fun execute(): LicenseCheckResult
}
