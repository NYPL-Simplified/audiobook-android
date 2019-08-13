package org.librarysimplified.audiobook.api

/**
 * A raw audio book manifest, parsed and typed.
 */

data class PlayerManifest(
  val spine: List<PlayerManifestSpineItem>,
  val metadata: PlayerManifestMetadata)

/**
 * The metadata section in a manifest.
 */

data class PlayerManifestMetadata(
  val title: String,
  val identifier: String,
  val encrypted: PlayerManifestEncrypted?)

/**
 * A section in a manifest dealing with encryption details.
 */

data class PlayerManifestEncrypted(
  val scheme: String,
  val values: Map<String, PlayerManifestScalar>)

/**
 * A spine item.
 */

data class PlayerManifestSpineItem(
  val values: Map<String, PlayerManifestScalar>)

/**
 * A scalar value appearing in a manifest.
 */

sealed class PlayerManifestScalar {

  /**
   * A string-typed scalar manifest value.
   */

  data class PlayerManifestScalarString(val text: String) : PlayerManifestScalar() {
    override fun toString(): String {
      return this.text
    }
  }

  /**
   * A number-typed scalar manifest value.
   */

  sealed class PlayerManifestScalarNumber : PlayerManifestScalar() {

    /**
     * A real-typed scalar manifest value.
     */

    data class PlayerManifestScalarReal(val number: Double) : PlayerManifestScalarNumber() {
      override fun toString(): String {
        return this.number.toString()
      }
    }

    /**
     * An integer-typed scalar manifest value.
     */

    data class PlayerManifestScalarInteger(val number: Int) : PlayerManifestScalarNumber() {
      override fun toString(): String {
        return this.number.toString()
      }
    }
  }

  /**
   * A boolean-typed scalar manifest value.
   */

  data class PlayerManifestScalarBoolean(val value: Boolean) : PlayerManifestScalar() {
    override fun toString(): String {
      return this.value.toString()
    }
  }
}