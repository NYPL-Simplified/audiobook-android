package org.nypl.audiobook.android.views

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.nypl.audiobook.android.views.PlayerSleepTimerConfiguration.*

/**
 * A Recycler view adapter used to display and control a sleep timer configuration menu.
 */

class PlayerSleepTimerAdapter(
  private val context: Context,
  private val rates: List<PlayerSleepTimerConfiguration>,
  private val onSelect: (PlayerSleepTimerConfiguration) -> Unit)
  : RecyclerView.Adapter<PlayerSleepTimerAdapter.ViewHolder>() {

  private val listener: View.OnClickListener

  init {
    this.listener = View.OnClickListener {
      v -> this.onSelect(v.tag as PlayerSleepTimerConfiguration)
    }
  }

  companion object {

    fun textOfConfiguration(
      resources: Resources,
      item: PlayerSleepTimerConfiguration): String {
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
        NEVER ->
          resources.getString(R.string.audiobook_player_sleep_never)
      }
    }

    fun contentDescriptionOf(
      resources: Resources,
      item: PlayerSleepTimerConfiguration): CharSequence {

      return when (item) {
        NEVER ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_off)
        END_OF_CHAPTER ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_end_of_chapter)
        MINUTES_60 ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_60_minutes)
        MINUTES_45 ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_45_minutes)
        MINUTES_30 ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_30_minutes)
        MINUTES_15 ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_15_minutes)
        NOW ->
          resources.getString(R.string.audiobook_accessibility_sleep_timer_now)
      }
    }

  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int): ViewHolder {

    UIThread.checkIsUIThread()

    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.player_sleep_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int) {

    UIThread.checkIsUIThread()

    val item = this.rates[position]
    holder.text.text =
      textOfConfiguration(resources = this.context.resources, item = item)
    holder.view.contentDescription =
      contentDescriptionOf(resources = this.context.resources, item = item)

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerSleepTimerAdapter.listener)
  }

  override fun getItemCount(): Int = this.rates.size

  inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val text: TextView = view.findViewById(R.id.player_sleep_item_view_name)
  }
}
