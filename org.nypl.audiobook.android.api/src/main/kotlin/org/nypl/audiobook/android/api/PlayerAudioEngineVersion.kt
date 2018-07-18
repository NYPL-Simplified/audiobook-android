package org.nypl.audiobook.android.api

/**
 * The version of an engine provider.
 */

data class PlayerAudioEngineVersion(
  val major: Int,
  val minor: Int,
  val patch: Int) : Comparable<PlayerAudioEngineVersion> {

  override fun compareTo(other: PlayerAudioEngineVersion): Int {
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

}
