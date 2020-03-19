package org.librarysimplified.audiobook.manifest_fulfill.opa

import java.net.URI

/**
 * The manifest URI.
 */

sealed class OPAManifestURI {

  /**
   * The given URI serves a manifest directly, using the provided OPA scope.
   *
   * @see "https://developer.overdrive.com/apis/patron-auth"
   */

  data class Direct(
    val targetURI: URI,
    val scope: String
  ) : OPAManifestURI()

  /**
   * The given URI serves a link to the real manifest, and also provides a scope
   * value in an `X-Overdrive-Scope` header.
   */

  data class Indirect(
    val targetURI: URI
  ) : OPAManifestURI()
}
