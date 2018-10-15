package org.nypl.audiobook.android.views

import android.widget.ImageView
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerType

/**
 * The listener interface implemented by activities hosting the various fragments included with
 * this package.
 */

interface PlayerFragmentListenerType {

  /**
   * Called when the player wants access to a player instance. The player should be created once
   * by the hosting activity and the same instance should be returned here each time this method
   * is called.
   */

  fun onPlayerWantsPlayer(): PlayerType

  /**
   * A fragment has created an image view representing a book cover image. The receiver must
   * now do whatever work is required to load the actual cover image into the given image view.
   */

  fun onPlayerWantsCoverImage(view: ImageView)

  /**
   * A fragment wants to know the title of the audio book being played. The receiver must return
   * the title of the book.
   */

  fun onPlayerWantsTitle(): String

  /**
   * A fragment wants to know the name of the author(s) of the audio book being played. The
   * receiver must return the name(s).
   */

  fun onPlayerWantsAuthor(): String

  /**
   * Called when the player wants access to a sleep timer instance. The sleep timer should be
   * created once by the hosting activity and the same instance should be returned here each time
   * this method is called.
   */

  fun onPlayerWantsSleepTimer(): PlayerSleepTimerType

  /**
   * The user has performed an action that requires that the TOC be opened. The caller should
   * load a fragment capable of displaying the TOC
   * (such as {@link org.nypl.audiobook.demo.android.views.PlayerTOCFragment}).
   */

  fun onPlayerTOCShouldOpen()

  /**
   * The loaded TOC fragment wants access to the audio book currently playing.
   */

  fun onPlayerTOCWantsBook(): PlayerAudioBookType

  /**
   * The user has closed the table of contents. The callee should remove the TOC fragment from
   * the hosting activity.
   */

  fun onPlayerTOCWantsClose()

  /**
   * The user triggered an action in the table of contents that wants to cancel all downloads. The
   * given function should be called to confirm deletion.
   */

  fun onPlayerTOCWantsCancelAllDownloads(confirm: () -> Unit)

  /**
   * The user has performed an action that requires that a playback rate selection dialog be opened.
   * The caller should load a fragment capable of displaying the rate selection menu
   * (such as {@link org.nypl.audiobook.demo.android.views.PlayerPlaybackRateFragment}).
   */

  fun onPlayerPlaybackRateShouldOpen()

  /**
   * The user has performed an action that requires that a sleep timer configuration dialog be opened.
   * The caller should load a fragment capable of displaying the configuration menu
   * (such as {@link org.nypl.audiobook.android.views.PlayerSleepTimerFragment}).
   */

  fun onPlayerSleepTimerShouldOpen()

}
