package org.nypl.audiobook.android.views

import android.support.annotation.ColorInt
import java.io.Serializable

/**
 * Parameters for the main player fragment.
 */

data class PlayerFragmentParameters(

  /**
   * The primary color used to tint various views in the player.
   */

  @ColorInt val primaryColor: Int) : Serializable
