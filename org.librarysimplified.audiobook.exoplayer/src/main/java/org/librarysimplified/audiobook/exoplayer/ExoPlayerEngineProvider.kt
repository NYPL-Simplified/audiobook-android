package org.librarysimplified.audiobook.exoplayer

import org.librarysimplified.audiobook.api.PlayerVersion
import org.librarysimplified.audiobook.api.PlayerVersions
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineProviderType
import org.librarysimplified.audiobook.player.api.PlayerAudioEngineRequest
import org.librarysimplified.audiobook.player.api.PlayerFactoryType
import org.slf4j.LoggerFactory

/**
 * An audio engine provider based on the Readium navigator.
 *
 * Note: This class MUST have a no-argument public constructor in order to be used via
 * java.util.ServiceLoader.
 */

class ExoPlayerEngineProvider : PlayerAudioEngineProviderType {

  private val log = LoggerFactory.getLogger(ExoPlayerEngineProvider::class.java)

  private val version: PlayerVersion =
    PlayerVersions.ofPropertiesClassOrNull(
      clazz = ExoPlayerEngineProvider::class.java,
      path = "/org/librarysimplified/audiobook/rbdigital/provider.properties"
    ) ?: PlayerVersion(0, 0, 0)

  override fun tryRequest(request: PlayerAudioEngineRequest): PlayerFactoryType? {
    val manifest = request.manifest
    val encrypted = manifest.metadata.encrypted
    if (encrypted != null) {
      this.log.debug("cannot open encrypted books")
      return null
    }

    return ExoPlayerFactory(
      context = request.context,
      publication = request.publication,
      bookID = request.bookID
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
