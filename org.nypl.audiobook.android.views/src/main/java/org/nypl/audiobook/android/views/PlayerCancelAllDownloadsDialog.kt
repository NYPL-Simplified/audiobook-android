package org.nypl.audiobook.android.views

import android.app.Activity
import android.app.AlertDialog

object PlayerCancelAllDownloadsDialog {

  fun create(
    activity: Activity,
    theme: Int,
    confirm: () -> Unit): AlertDialog {
    val builder = AlertDialog.Builder(activity, theme)
    builder.setMessage(R.string.audiobook_player_toc_menu_stop_all_confirm)
      .setPositiveButton(
        R.string.audiobook_player_toc_menu_stop_confirm_positive,
        { dialog, id -> confirm.invoke() })
      .setNegativeButton(
        R.string.audiobook_player_toc_menu_stop_confirm_negative,
        { dialog, id -> })
    return builder.create()
  }

}