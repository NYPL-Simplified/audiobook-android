package org.nypl.audiobook.android.api

import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * An API to find engine providers for books.
 */

object PlayerAudioEngines : PlayerAudioEnginesType {

  private val log = LoggerFactory.getLogger(PlayerAudioEngines::class.java)

  private val providers : MutableList<PlayerAudioEngineProviderType> =
    ServiceLoader.load(PlayerAudioEngineProviderType::class.java).toMutableList()

  override fun findAllFor(request: PlayerAudioEngineRequest): List<PlayerEngineAndBookProvider> {
    val results = ArrayList<PlayerEngineAndBookProvider>(this.providers.size)
    for (engine_provider in this.providers) {
      try {
        val book_provider = engine_provider.tryRequest(request)
        if (book_provider != null) {
          if (request.filter(engine_provider)) {
            results.add(PlayerEngineAndBookProvider(
              engineProvider = engine_provider,
              bookProvider = book_provider))
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
