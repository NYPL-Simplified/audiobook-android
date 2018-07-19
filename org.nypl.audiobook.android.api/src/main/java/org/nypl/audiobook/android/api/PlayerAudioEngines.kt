package org.nypl.audiobook.android.api

import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * An API to find engine providers for books.
 */

object PlayerAudioEngines : PlayerAudioEnginesType {

  private val log = LoggerFactory.getLogger(PlayerAudioEngines::class.java)

  private val providers : MutableList<PlayerAudioEngineProviderType>

  init {
    this.providers = ServiceLoader.load(PlayerAudioEngineProviderType::class.java).toMutableList()
  }

  override fun findAllFor(
    manifest: PlayerManifest,
    filter: (PlayerAudioEngineProviderType) -> Boolean): List<PlayerEngineAndBook> {

    val results = ArrayList<PlayerEngineAndBook>(this.providers.size)
    for (engine_provider in this.providers) {
      try {
        val book_provider = engine_provider.canSupportBook(manifest)
        if (book_provider != null) {
          if (filter(engine_provider)) {
            results.add(PlayerEngineAndBook(engine = engine_provider, book = book_provider))
          }
        }
      } catch (e: Exception) {
        try {
          this.log.error(
            "exception raised by provider {}:{} when examining manifest: ",
            engine_provider.name(),
            engine_provider.version(),
            e)
        } catch (e: Exception) {
          this.log.error("exception raised when talking to provider: ", e)
        }
      }
    }

    return results
  }
}
