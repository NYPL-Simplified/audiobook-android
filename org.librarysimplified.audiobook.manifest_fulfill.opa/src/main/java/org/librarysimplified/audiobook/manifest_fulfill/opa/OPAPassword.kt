package org.librarysimplified.audiobook.manifest_fulfill.opa

import java.io.Serializable

/**
 * An Overdrive Patron Authentication password.
 */

sealed class OPAPassword : Serializable {

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
