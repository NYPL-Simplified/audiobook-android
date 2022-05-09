package org.librarysimplified.audiobook.open_access

import org.librarysimplified.audiobook.api.PlayerAudioBookProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineProviderType
import org.librarysimplified.audiobook.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.api.PlayerVersion
import org.librarysimplified.audiobook.api.PlayerVersions
import org.slf4j.LoggerFactory

/**
 * An audio engine provider based on the Readium navigator.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class ReadiumEngineProvider : PlayerAudioEngineProviderType {

  private val log = LoggerFactory.getLogger(ReadiumEngineProvider::class.java)

  private val version: PlayerVersion =
    PlayerVersions.ofPropertiesClassOrNull(
      clazz = ReadiumEngineProvider::class.java,
      path = "/org/librarysimplified/audiobook/rbdigital/provider.properties"
    ) ?: PlayerVersion(0, 0, 0)

  override fun tryRequest(request: PlayerAudioEngineRequest): PlayerAudioBookProviderType? {
    val manifest = request.manifest
    val encrypted = manifest.metadata.encrypted
    if (encrypted != null || request.downloadManifest == null) {
      this.log.debug("cannot open encrypted books")
      return null
    }

    return ReadiumAudioBookProvider(
      manifest = manifest,
      downloadManifest = request.downloadManifest!!
    )
  }

  override fun name(): String {
    return "org.librarysimplified.audiobook.open_access"
  }

  override fun version(): PlayerVersion {
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
}
