package org.librarysimplified.audiobook.http

import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
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

  /**
   * The default HTTP client.
   */

  fun defaultClient(): OkHttpClient =
    this.defaultClient
}
