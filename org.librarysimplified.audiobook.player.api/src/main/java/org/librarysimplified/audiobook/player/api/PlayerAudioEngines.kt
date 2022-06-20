package org.librarysimplified.audiobook.player.api

import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * An API to find engine providers for books.
 */

object PlayerAudioEngines : PlayerAudioEnginesType {

  private val logger = LoggerFactory.getLogger(PlayerAudioEngines::class.java)

  private val providers: MutableList<PlayerAudioEngineProviderType> =
    ServiceLoader.load(PlayerAudioEngineProviderType::class.java).toMutableList()

  override fun findBestFor(request: PlayerAudioEngineRequest): PlayerFactoryType? {
    val results = ArrayList<Pair<PlayerAudioEngineProviderType, PlayerFactoryType>>(this.providers.size)
    for (engineProvider in this.providers) {
      try {
        val playerFactory = engineProvider.tryRequest(request)
        if (playerFactory != null) {
          if (request.filter(engineProvider)) {
            results.add(engineProvider to playerFactory)
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

    return results.maxByOrNull { pair -> pair.first.version() }?.second
  }
}
