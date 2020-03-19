package org.librarysimplified.audiobook.tests

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.librarysimplified.audiobook.license_check.api.LicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType
import org.librarysimplified.audiobook.manifest.api.PlayerManifest
import org.librarysimplified.audiobook.manifest_parser.api.ManifestParsers
import org.librarysimplified.audiobook.manifest_parser.extension_spi.ManifestParserExtensionType
import org.librarysimplified.audiobook.parser.api.ParseResult
import org.slf4j.Logger
import java.io.IOException
import java.net.URI
import java.util.ServiceLoader

abstract class LicenseCheckContract {

  private lateinit var eventLog: MutableList<SingleLicenseCheckStatus>

  abstract fun log(): Logger

  abstract fun licenseChecks(): LicenseCheckProviderType

  @Before
  fun testSetup() {
    this.eventLog = mutableListOf()
  }

  /**
   * An empty list of license checks trivially succeeds.
   */

  @Test
  fun testEmptySucceeds() {
    val checks = this.licenseChecks()
    val manifest = this.manifest("ok_minimal_0.json")

    val result =
      checks.createLicenseCheck(manifest, listOf()).use { check ->
        check.events.subscribe { event -> this.eventLog.add(event) }
        check.execute()
      }

    Assert.assertEquals(0, result.checkStatuses.size)
    Assert.assertTrue(result.checkSucceeded())
  }

  /**
   * A list of license checks with a single failing check fails.
   */

  @Test
  fun testOneFails() {
    val checks = this.licenseChecks()
    val manifest = this.manifest("ok_minimal_0.json")

    val result =
      checks.createLicenseCheck(
        manifest, listOf(
          SucceedingTest(),
          SucceedingTest(),
          FailingTest(),
          SucceedingTest()
        )
      ).use { check ->
        check.events.subscribe { event -> this.eventLog.add(event) }
        check.execute()
      }

    Assert.assertEquals(4, result.checkStatuses.size)
    Assert.assertFalse(result.checkSucceeded())
  }

  /**
   * A crashing check is treated as if the check failed.
   */

  @Test
  fun testOneCrashes() {
    val checks = this.licenseChecks()
    val manifest = this.manifest("ok_minimal_0.json")

    val result =
      checks.createLicenseCheck(
        manifest, listOf(
          SucceedingTest(),
          SucceedingTest(),
          CrashingTest(),
          SucceedingTest()
        )
      ).use { check ->
        check.events.subscribe { event -> this.eventLog.add(event) }
        check.execute()
      }

    Assert.assertEquals(4, result.checkStatuses.size)
    Assert.assertFalse(result.checkSucceeded())
  }

  /**
   * Non applicable tests succeed.
   */

  @Test
  fun testNonApplicable() {
    val checks = this.licenseChecks()
    val manifest = this.manifest("ok_minimal_0.json")

    val result =
      checks.createLicenseCheck(
        manifest, listOf(
          NonApplicableTest(),
          NonApplicableTest(),
          NonApplicableTest(),
          NonApplicableTest()
        )
      ).use { check ->
        check.events.subscribe { event -> this.eventLog.add(event) }
        check.execute()
      }

    Assert.assertEquals(4, result.checkStatuses.size)
    Assert.assertTrue(result.checkSucceeded())
  }

  private class NonApplicableTest : SingleLicenseCheckType, SingleLicenseCheckProviderType {
    override fun execute(): SingleLicenseCheckResult {
      return SingleLicenseCheckResult.NotApplicable("NotApplicable!")
    }

    override val name: String
      get() = "NonApplicable"

    override fun createLicenseCheck(
      manifest: PlayerManifest,
      onStatusChanged: (SingleLicenseCheckStatus) -> Unit
    ): SingleLicenseCheckType {
      return this
    }
  }

  private class SucceedingTest : SingleLicenseCheckType, SingleLicenseCheckProviderType {
    override fun execute(): SingleLicenseCheckResult {
      return SingleLicenseCheckResult.Succeeded("Succeeded!")
    }

    override val name: String
      get() = "Succeeding"

    override fun createLicenseCheck(
      manifest: PlayerManifest,
      onStatusChanged: (SingleLicenseCheckStatus) -> Unit
    ): SingleLicenseCheckType {
      return this
    }
  }

  private class FailingTest : SingleLicenseCheckType, SingleLicenseCheckProviderType {
    override fun execute(): SingleLicenseCheckResult {
      return SingleLicenseCheckResult.Failed("Failed!", null)
    }

    override val name: String
      get() = "Failing"

    override fun createLicenseCheck(
      manifest: PlayerManifest,
      onStatusChanged: (SingleLicenseCheckStatus) -> Unit
    ): SingleLicenseCheckType {
      return this
    }
  }

  private class CrashingTest : SingleLicenseCheckType, SingleLicenseCheckProviderType {
    override fun execute(): SingleLicenseCheckResult {
      throw IOException("Crashing!")
    }

    override val name: String
      get() = "Crashing"

    override fun createLicenseCheck(
      manifest: PlayerManifest,
      onStatusChanged: (SingleLicenseCheckStatus) -> Unit
    ): SingleLicenseCheckType {
      return this
    }
  }

  private fun manifest(
    name: String
  ): PlayerManifest {
    val result =
      ManifestParsers.parse(
        uri = URI.create(name),
        streams = this.resource(name),
        extensions = ServiceLoader.load(ManifestParserExtensionType::class.java).toList()
      )
    this.log().debug("result: {}", result)
    Assert.assertTrue("Result is success", result is ParseResult.Success)

    val success: ParseResult.Success<PlayerManifest> =
      result as ParseResult.Success<PlayerManifest>

    return success.result
  }

  private fun resource(
    name: String
  ): ByteArray {
    val path = "/org/librarysimplified/audiobook/tests/" + name
    return LicenseCheckContract::class.java.getResourceAsStream(path)?.readBytes()
      ?: throw AssertionError("Missing resource file: " + path)
  }
}
