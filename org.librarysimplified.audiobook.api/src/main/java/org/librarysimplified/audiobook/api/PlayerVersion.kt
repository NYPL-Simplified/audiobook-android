package org.librarysimplified.audiobook.api

/**
 * A semantic version number.
 */

data class PlayerVersion(
  val major: Int,
  val minor: Int,
  val patch: Int
) : Comparable<PlayerVersion> {

  override fun compareTo(other: PlayerVersion): Int {
    val c_major = this.major.compareTo(other.major)
    if (c_major == 0) {
      val c_minor = this.minor.compareTo(other.minor)
      if (c_minor == 0) {
        val c_patch = this.patch.compareTo(other.patch)
        if (c_patch == 0) {
          return 0
        }
        return c_patch
      }
      return c_minor
    }
    return c_major
  }

  override fun toString(): String {
    return "${this.major}.${this.minor}.${this.patch}"
  }
}
