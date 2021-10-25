package org.librarysimplified.audiobook.views

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.END_OF_CHAPTER
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.MINUTES_15
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.MINUTES_30
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.MINUTES_45
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.MINUTES_60
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.NOW
import org.librarysimplified.audiobook.views.PlayerSleepTimerConfiguration.OFF

/**
 * A Recycler view adapter used to display and control a sleep timer configuration menu.
 */

class PlayerSleepTimerAdapter(
  private val context: Context,
  private val rates: List<PlayerSleepTimerConfiguration>,
  private val onSelect: (PlayerSleepTimerConfiguration) -> Unit
) :
  RecyclerView.Adapter<PlayerSleepTimerAdapter.ViewHolder>() {

  private val listener: View.OnClickListener = View.OnClickListener { v ->
    this.onSelect(v.tag as PlayerSleepTimerConfiguration)
  }

  companion object {

    fun textOfConfiguration(
      resources: Resources,
      item: PlayerSleepTimerConfiguration
    ): String {
      return when (item) {
        END_OF_CHAPTER ->
          resources.getString(R.string.audiobook_player_sleep_end_of_chapter)
        MINUTES_60 ->
          resources.getString(R.string.audiobook_player_sleep_60)
        MINUTES_45 ->
          resources.getString(R.string.audiobook_player_sleep_45)
        MINUTES_30 ->
          resources.getString(R.string.audiobook_player_sleep_30)
        MINUTES_15 ->
          resources.getString(R.string.audiobook_player_sleep_15)
        NOW ->
          resources.getString(R.string.audiobook_player_sleep_now)
        OFF ->
          resources.getString(R.string.audiobook_player_sleep_off)
      }
    }

    private fun menuItemContentDescription(
      resources: Resources,
      item: PlayerSleepTimerConfiguration
    ): String {

      return when (item) {
        OFF ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_off)
        END_OF_CHAPTER ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_at_end_of_chapter)
        MINUTES_60 ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_in_60_minutes)
        MINUTES_45 ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_in_45_minutes)
        MINUTES_30 ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_in_30_minutes)
        MINUTES_15 ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_in_15_minutes)
        NOW ->
          resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_item_now)
      }
    }

    fun hasBeenSetToContentDescriptionOf(
      resources: Resources,
      item: PlayerSleepTimerConfiguration
    ): String {
      return StringBuilder(64)
        .append(resources.getString(R.string.audiobook_accessibility_sleep_timer_has_been_set))
        .append(" ")
        .append(menuItemContentDescription(resources, item))
        .toString()
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    UIThread.checkIsUIThread()

    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.player_sleep_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {

    UIThread.checkIsUIThread()

    val item = this.rates[position]
    holder.text.text =
      textOfConfiguration(resources = this.context.resources, item = item)
    holder.view.contentDescription =
      menuItemContentDescription(resources = this.context.resources, item = item)

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerSleepTimerAdapter.listener)
  }

  override fun getItemCount(): Int = this.rates.size

  inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val text: TextView = view.findViewById(R.id.player_sleep_item_view_name)
  }
}
