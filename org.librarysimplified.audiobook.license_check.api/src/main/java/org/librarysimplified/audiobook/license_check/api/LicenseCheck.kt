package org.librarysimplified.audiobook.license_check.api

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.slf4j.LoggerFactory

internal class LicenseCheck internal constructor(
  private val parameters: LicenseCheckParameters
) : LicenseCheckType {

  private val logger =
    LoggerFactory.getLogger(LicenseChecks::class.java)

  private val eventSubject =
    PublishSubject.create<SingleLicenseCheckStatus>()

  override val events: Observable<SingleLicenseCheckStatus> =
    this.eventSubject

  override fun execute(): LicenseCheckResult {
    val results = mutableListOf<SingleLicenseCheckResult>()
    for (checkProvider in this.parameters.checks) {
      this.logger.debug("[{}]: executing", checkProvider.name)

      val checkResult = try {
        val singleCheck =
          checkProvider.createLicenseCheck(
            SingleLicenseCheckParameters(
              manifest = this.parameters.manifest,
              userAgent = this.parameters.userAgent,
              onStatusChanged = this.eventSubject::onNext,
              cacheDirectory = this.parameters.cacheDirectory
            )
          )
        singleCheck.execute()
      } catch (e: Exception) {
        this.logger.error("[{}]: failed: ", checkProvider.name, e)
        SingleLicenseCheckResult.Failed(
          e.message ?: e.javaClass.name,
          e
        )
      }

      this.logger.debug(
        "[{}]: result: {} - {}",
        checkProvider.name,
        checkResult.shortName,
        checkResult.message
      )
      results.add(checkResult)
    }

    assert(results.size == this.parameters.checks.size)
    return LicenseCheckResult(
      results.toList()
    )
  }

  override fun close() {
    this.eventSubject.onComplete()
  }
}
