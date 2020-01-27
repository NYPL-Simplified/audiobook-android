package org.librarysimplified.audiobook.api

import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.Exception
import java.util.Properties

/**
 * Functions to serialize/deserialize player versions.
 */

object PlayerVersions {

  private val logger =
    LoggerFactory.getLogger(PlayerVersions::class.java)

  /**
   * Load version information from the given path via the classloader of `clazz`, throwing
   * exceptions on failure.
   */

  @Throws(Exception::class)
  fun ofPropertiesClass(
    clazz: Class<*>,
    path: String
  ): PlayerVersion {
    val url = clazz.getResource(path) ?: throw FileNotFoundException(path)
    return url.openStream().use(this::ofPropertiesStream)
  }

  /**
   * Load version information from the given path via the classloader of `clazz`, logging errors
   * and returning `null` on failure.
   */

  @Throws(Exception::class)
  fun ofPropertiesClassOrNull(
    clazz: Class<*>,
    path: String
  ): PlayerVersion? {
    val url = clazz.getResource(path) ?: return null
    return url.openStream().use(this::ofPropertiesStreamOrNull)
  }

  /**
   * Load version information from the given stream, throwing exceptions on failure.
   */

  @Throws(Exception::class)
  fun ofPropertiesStream(stream: InputStream): PlayerVersion {
    val properties = Properties()
    properties.load(stream)
    return this.ofProperties(properties)
  }

  /**
   * Load version information from the given stream, logging errors and returning `null` on failure.
   */

  fun ofPropertiesStreamOrNull(stream: InputStream): PlayerVersion? {
    return try {
      this.ofPropertiesStream(stream)
    } catch (e: Exception) {
      this.logger.error("could not load property stream: ", e)
      null
    }
  }

  /**
   * Load version information from the given properties.
   */

  fun ofProperties(properties: Properties): PlayerVersion {
    val major = properties.getProperty("version.major").toIntOrNull() ?: 0
    val minor = properties.getProperty("version.minor").toIntOrNull() ?: 0
    val patch = properties.getProperty("version.patch").toIntOrNull() ?: 0
    return PlayerVersion(
      major = major,
      minor = minor,
      patch = patch
    )
  }
}