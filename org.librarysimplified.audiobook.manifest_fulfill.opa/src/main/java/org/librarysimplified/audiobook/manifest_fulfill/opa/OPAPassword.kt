package org.librarysimplified.audiobook.manifest_fulfill.opa

/**
 * An Overdrive Patron Authentication password.
 */

sealed class OPAPassword {

  /**
   * A password is not required.
   */

  object NotRequired : OPAPassword()

  /**
   * A password is required, and is provided.
   */

  data class Password(
    val password: String
  ) : OPAPassword()
}
