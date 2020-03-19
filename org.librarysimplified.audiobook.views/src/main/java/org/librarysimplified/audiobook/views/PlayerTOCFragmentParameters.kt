package org.librarysimplified.audiobook.views

import androidx.annotation.ColorInt
import java.io.Serializable

/**
 * Parameters for the table of contents fragment.
 */

class PlayerTOCFragmentParameters(

  /**
   * The primary color used to tint various views in the table. Notably, this is the color used
   * as the border color for the active table item.
   */

  @Deprecated(
    message = "Colors should now be taken from the colorPrimary attribute of the current application theme",
    level = DeprecationLevel.WARNING)
  @ColorInt val primaryColor: Int? = null
) : Serializable
