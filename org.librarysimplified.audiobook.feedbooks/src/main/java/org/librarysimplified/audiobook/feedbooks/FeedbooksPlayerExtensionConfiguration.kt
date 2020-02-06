package org.librarysimplified.audiobook.feedbooks

/**
 * Configuration information for the Feedbooks player extension.
 */

data class FeedbooksPlayerExtensionConfiguration(

  /**
   * The secret shared between the book distributor and this user agent.
   */

  val bearerTokenSecret: String,

  /**
   * A URL controlled by the user agent.
   */

  val issuerURL: String
)
