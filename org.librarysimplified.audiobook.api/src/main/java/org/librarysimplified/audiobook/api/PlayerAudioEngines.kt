package org.librarysimplified.audiobook.api

import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * An API to find engine providers for books.
 */

object PlayerAudioEngines : PlayerAudioEnginesType {

  private val logger = LoggerFactory.getLogger(PlayerAudioEngines::class.java)

  private val providers: MutableList<PlayerAudioEngineProviderType> =
    ServiceLoader.load(PlayerAudioEngineProviderType::class.java).toMutableList()

  override fun findAllFor(request: PlayerAudioEngineRequest): List<PlayerEngineAndBookProvider> {
    val results = ArrayList<PlayerEngineAndBookProvider>(this.providers.size)
    for (engineProvider in this.providers) {
      try {
        val bookProvider = engineProvider.tryRequest(request)
        if (bookProvider != null) {
          if (request.filter(engineProvider)) {
            results.add(
              PlayerEngineAndBookProvider(
                engineProvider = engineProvider,
                bookProvider = bookProvider
              )
            )
          }
        }
      } catch (e: Exception) {
        try {
          this.logger.error(
            "exception raised by provider {}:{} when examining manifest: ",
            engineProvider.name(),
            engineProvider.version(),
            e
          )
        } catch (e: Exception) {
          this.logger.error("exception raised when talking to provider: ", e)
        }
      }
    }

    return results
  }
}
