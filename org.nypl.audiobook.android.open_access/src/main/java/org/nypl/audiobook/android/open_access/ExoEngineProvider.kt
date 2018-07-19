package org.nypl.audiobook.android.open_access

import org.nypl.audiobook.android.api.PlayerAudioBookProviderType
import org.nypl.audiobook.android.api.PlayerAudioEngineProviderType
import org.nypl.audiobook.android.api.PlayerAudioEngineVersion
import org.nypl.audiobook.android.api.PlayerManifest
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.Properties

/**
 * An audio engine provider based on ExoPlayer.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class ExoEngineProvider : PlayerAudioEngineProviderType {

  private val version: PlayerAudioEngineVersion

  init {
    this.version = parseVersionFromProperties()
  }

  override fun canSupportBook(manifest: PlayerManifest): PlayerAudioBookProviderType? {
    val encrypted = manifest.metadata.encrypted
    if (encrypted != null) {
      log.debug("cannot open encrypted books")
      return null
    }

    return ExoAudioBookProvider(manifest)
  }

  override fun name(): String {
    return "org.nypl.audiobook.android.open_access"
  }

  override fun version(): PlayerAudioEngineVersion {
    return this.version
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