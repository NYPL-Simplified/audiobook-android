package org.librarysimplified.audiobook.http

import okhttp3.Cache
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The default HTTP clients.
 */

object AudioBookHTTPClients {

  private val logger =
    LoggerFactory.getLogger(AudioBookHTTPClients::class.java)

  private val defaultClient =
    OkHttpClient.Builder()
      .connectTimeout(3L, TimeUnit.MINUTES)
      .callTimeout(3L, TimeUnit.MINUTES)
      .addInterceptor(AudioBookHTTPInterceptor(this.logger))
      .build()

  private val cachingClients: HashMap<String, OkHttpClient> = hashMapOf()

  /**
   * The default HTTP client.
   */

  fun defaultClient(): OkHttpClient =
    this.defaultClient

  /**
   * A caching HTTP client.
   */

  fun cachingClient(cacheDirectory: File): OkHttpClient {
    // If a client has already been configured for the given cache directory, return it.

    val cachePath = cacheDirectory.canonicalPath
    val client = cachingClients[cachePath]

    if (client != null) {
      return client
    }

    // Otherwise, configure a caching client based on the default client, and memoize it.

    return defaultClient.newBuilder()
      .cache(
        Cache(
          File(cacheDirectory, "audiobook-http"),
          // A small (16 KB) cache should suffice, as the cache is currently only used for a feedbooks
          // certificate (about 1 KB).
          16L * 1024L
        )
      ).build()
      .also {
        cachingClients.put(cachePath, it)
      }
  }
}
