package org.nypl.audiobook.android.views

import android.support.annotation.ColorInt
import java.io.Serializable

/**
 * Parameters for the table of contents fragment.
 */

class PlayerTOCFragmentParameters(

  /**
   * The primary color used to tint various views in the table. Notably, this is the color used
   * as the border color for the active table item.
   */

  @ColorInt val primaryColor: Int) : Serializable
