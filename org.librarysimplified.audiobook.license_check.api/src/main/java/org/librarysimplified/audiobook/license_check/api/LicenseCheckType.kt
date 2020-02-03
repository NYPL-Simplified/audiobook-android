package org.librarysimplified.audiobook.license_check.api

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import rx.Observable
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
