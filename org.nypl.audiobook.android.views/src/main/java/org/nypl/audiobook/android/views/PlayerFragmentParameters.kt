package org.nypl.audiobook.android.views

import android.support.annotation.ColorInt
import java.io.Serializable

/**
 * Parameters for the main player fragment.
 */

data class PlayerFragmentParameters(

  /**
   * If this value is set to `true`, then the user may cancel individual downloads in the TOC.
   * Note that if this is set to `true`, then the views will use the audio book's individual
   * download tasks to start and stop chapter downloads. If this is set to `false`, the views
   * will use the whole-book download task. It is important that you use the correct API in
   * combination with this flag, because the API gives few guarantees about how the whole-book
   * and individual-chapter download tasks can interoperate.
   */

  val allowIndividualDownloadCancellations: Boolean,

  /**
   * The primary color used to tint various views in the player.
   */

  @ColorInt val primaryColor: Int) : Serializable
