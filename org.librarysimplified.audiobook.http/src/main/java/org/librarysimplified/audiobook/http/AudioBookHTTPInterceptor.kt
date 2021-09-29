package org.librarysimplified.audiobook.http

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.Logger

/**
 * A trivial logging interceptor.
 */

class AudioBookHTTPInterceptor(
  private val logger: Logger
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    this.logger.debug("request: {}", chain.request().url)
    return chain.proceed(chain.request())
  }
}
