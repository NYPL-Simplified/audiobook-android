package org.librarysimplified.audiobook.views

import android.content.res.Resources
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.DOUBLE_TIME
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.NORMAL_TIME
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.ONE_AND_A_HALF_TIME
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.ONE_AND_A_QUARTER_TIME
import org.librarysimplified.audiobook.api.PlayerPlaybackRate.THREE_QUARTERS_TIME

/**
 * A Recycler view adapter used to display and control a playback rate configuration menu.
 */

class PlayerPlaybackRateAdapter(
  private val resources: Resources,
  private val rates: List<PlayerPlaybackRate>,
  private val onSelect: (PlayerPlaybackRate) -> Unit
) :
  RecyclerView.Adapter<PlayerPlaybackRateAdapter.ViewHolder>() {

  private val listener: View.OnClickListener =
    View.OnClickListener { v -> this.onSelect(v.tag as PlayerPlaybackRate) }

  private var currentRate: PlayerPlaybackRate =
    NORMAL_TIME

  companion object {

    fun textOfRate(item: PlayerPlaybackRate): String {
      return when (item) {
        THREE_QUARTERS_TIME -> "0.75x"
        NORMAL_TIME -> "1.0x"
        ONE_AND_A_QUARTER_TIME -> "1.25x"
        ONE_AND_A_HALF_TIME -> "1.5x"
        DOUBLE_TIME -> "2.0x"
      }
    }

    fun menuItemContentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate
    ): String {

      return StringBuilder(128)
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_set_to))
        .append(" ")
        .append(contentDescriptionOfRate(resources, item))
        .toString()
    }

    fun contentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate
    ): String {
      return when (item) {
        THREE_QUARTERS_TIME ->
          resources.getString(R.string.audiobook_accessibility_menu_playback_speed_0p75)
        NORMAL_TIME ->
          resources.getString(R.string.audiobook_accessibility_menu_playback_speed_1)
        ONE_AND_A_QUARTER_TIME ->
          resources.getString(R.string.audiobook_accessibility_menu_playback_speed_1p25)
        ONE_AND_A_HALF_TIME ->
          resources.getString(R.string.audiobook_accessibility_menu_playback_speed_1p5)
        DOUBLE_TIME ->
          resources.getString(R.string.audiobook_accessibility_menu_playback_speed_2)
      }
    }

    fun hasBeenSetToContentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate
    ): String {
      return StringBuilder(128)
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_has_been_set))
        .append(" ")
        .append(contentDescriptionOfRate(resources, item))
        .toString()
    }

    fun menuItemSelectedContentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate
    ): String {
      return StringBuilder(128)
        .append(menuItemContentDescriptionOfRate(resources, item))
        .append(". ")
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_is_selected))
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
        .inflate(R.layout.player_rate_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {

    UIThread.checkIsUIThread()

    val item = this.rates[position]
    holder.text.text = textOfRate(item)

    if (item == this.currentRate) {
      holder.view.contentDescription = menuItemSelectedContentDescriptionOfRate(this.resources, item)
      holder.view.isEnabled = false
      holder.text.isEnabled = false
    } else {
      holder.view.contentDescription = menuItemContentDescriptionOfRate(this.resources, item)
      holder.view.isEnabled = true
      holder.text.isEnabled = true
    }

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerPlaybackRateAdapter.listener)
  }

  override fun getItemCount(): Int = this.rates.size

  fun setCurrentPlaybackRate(rate: PlayerPlaybackRate) {
    this.currentRate = rate
    this.notifyDataSetChanged()
  }

  inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val text: TextView = view.findViewById(R.id.player_rate_item_view_name)
  }
}
