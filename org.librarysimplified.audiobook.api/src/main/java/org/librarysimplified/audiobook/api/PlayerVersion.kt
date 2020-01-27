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
    val cMajor = this.major.compareTo(other.major)
    if (cMajor == 0) {
      val cMinor = this.minor.compareTo(other.minor)
      if (cMinor == 0) {
        val cPatch = this.patch.compareTo(other.patch)
        if (cPatch == 0) {
          return 0
        }
        return cPatch
      }
      return cMinor
    }
    return cMajor
  }

  override fun toString(): String {
    return "${this.major}.${this.minor}.${this.patch}"
  }
}
