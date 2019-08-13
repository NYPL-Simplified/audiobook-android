package org.nypl.audiobook.android.open_access

import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerAudioEngineVersion
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * An audio engine provider based on ExoPlayer.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class ExoEngineProvider : PlayerAudioEngineProviderType {

  private val version: PlayerAudioEngineVersion = parseVersionFromProperties()

  private val engineExecutor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor({ r -> createEngineThread(r) })

  /**
   * Create a thread suitable for use with the ExoPlayer audio engine.
   */

  private fun createEngineThread(r: Runnable?): Thread {
    val thread = ExoEngineThread(r ?: Runnable { })
    thread.setUncaughtExceptionHandler { t, e ->
      log.error("uncaught exception on engine thread {}: ", t, e)
    }
    return thread
  }

  override fun tryRequest(request: PlayerAudioEngineRequest): PlayerAudioBookProviderType? {
    val manifest = request.manifest
    val encrypted = manifest.metadata.encrypted
    if (encrypted != null) {
      log.debug("cannot open encrypted books")
      return null
    }

    return ExoAudioBookProvider(
      engineProvider = this,
      engineExecutor = this.engineExecutor,
      downloadProvider = request.downloadProvider,
      manifest = manifest)
  }

  override fun name(): String {
    return "org.nypl.audiobook.android.open_access"
  }

  override fun version(): PlayerAudioEngineVersion {
    return this.version
  }

  override fun toString(): String {
    return StringBuilder(32)
      .append(this.name())
      .append(':')
      .append(this.version.major)
      .append('.')
      .append(this.version.minor)
      .append('.')
      .append(this.version.patch)
      .toString()
  }

  companion object {

    private val log = LoggerFactory.getLogger(ExoEngineProvider::class.java)

    private fun parseVersionFromProperties(): PlayerAudioEngineVersion {
      try {
        val path = "/org/nypl/audiobook/android/open_access/provider.properties"
        val stream = ExoEngineProvider::class.java.getResourceAsStream(path)
        if (stream == null) {
          throw IllegalStateException("Unable to load properties from: " + path)
        }
        return loadPropertiesFromStream(stream)
      } catch (e: Exception) {
        this.log.error("could not load properties: ", e)
        return PlayerAudioEngineVersion(0, 0, 0)
      }
    }

    private fun loadPropertiesFromStream(stream: InputStream): PlayerAudioEngineVersion {
      val props = Properties()
      props.load(stream)
      val major = Integer.parseInt(props.getProperty("version.major"))
      val minor = Integer.parseInt(props.getProperty("version.minor"))
      val patch = Integer.parseInt(props.getProperty("version.patch"))
      return PlayerAudioEngineVersion(major, minor, patch)
    }
  }
}