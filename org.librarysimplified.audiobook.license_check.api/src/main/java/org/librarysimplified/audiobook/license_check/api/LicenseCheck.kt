package org.librarysimplified.audiobook.license_check.api

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.slf4j.LoggerFactory
import rx.Observable
import rx.subjects.PublishSubject

internal class LicenseCheck internal constructor(
  private val manifest: PlayerManifest,
  private val checks: List<SingleLicenseCheckProviderType>
) : LicenseCheckType {

  private val logger =
    LoggerFactory.getLogger(LicenseChecks::class.java)

  private val eventSubject =
    PublishSubject.create<SingleLicenseCheckStatus>()

  override val events: Observable<SingleLicenseCheckStatus> =
    this.eventSubject

  override fun execute(): LicenseCheckResult {
    val results = mutableListOf<SingleLicenseCheckResult>()
    for (checkProvider in this.checks) {
      this.logger.debug("[{}]: executing", checkProvider.name)

      val checkResult = try {
        val singleCheck =
          checkProvider.createLicenseCheck(this.manifest, this.eventSubject::onNext)
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

    assert(results.size == this.checks.size)
    return LicenseCheckResult(
      results.toList()
    )
  }

  override fun close() {
    this.eventSubject.onCompleted()
  }
}