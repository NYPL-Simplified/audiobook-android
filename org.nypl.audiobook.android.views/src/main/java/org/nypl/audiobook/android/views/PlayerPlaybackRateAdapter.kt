package org.nypl.audiobook.android.views

import android.content.res.Resources
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerPlaybackRate.DOUBLE_TIME
import org.nypl.audiobook.android.api.PlayerPlaybackRate.NORMAL_TIME
import org.nypl.audiobook.android.api.PlayerPlaybackRate.ONE_AND_A_HALF_TIME
import org.nypl.audiobook.android.api.PlayerPlaybackRate.ONE_AND_A_QUARTER_TIME
import org.nypl.audiobook.android.api.PlayerPlaybackRate.THREE_QUARTERS_TIME
import java.lang.StringBuilder

/**
 * A Recycler view adapter used to display and control a playback rate configuration menu.
 */

class PlayerPlaybackRateAdapter(
  private val resources: Resources,
  private val rates: List<PlayerPlaybackRate>,
  private val parameters: PlayerFragmentParameters,
  private val onSelect: (PlayerPlaybackRate) -> Unit)
  : RecyclerView.Adapter<PlayerPlaybackRateAdapter.ViewHolder>() {

  private val listener: View.OnClickListener
  private var currentRate: PlayerPlaybackRate = NORMAL_TIME

  init {
    this.listener = View.OnClickListener { v -> this.onSelect(v.tag as PlayerPlaybackRate) }
  }

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
      item: PlayerPlaybackRate): String {

      return StringBuilder(128)
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_set_to))
        .append(" ")
        .append(this.contentDescriptionOfRate(resources, item))
        .toString()
    }

    fun contentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate): String {
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
      item: PlayerPlaybackRate): String {
      return StringBuilder(128)
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_has_been_set))
        .append(" ")
        .append(this.contentDescriptionOfRate(resources, item))
        .toString()
    }

    fun menuItemSelectedContentDescriptionOfRate(
      resources: Resources,
      item: PlayerPlaybackRate): String {
      return StringBuilder(128)
        .append(this.menuItemContentDescriptionOfRate(resources, item))
        .append(". ")
        .append(resources.getString(R.string.audiobook_accessibility_playback_speed_is_selected))
        .toString()
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int): ViewHolder {

    UIThread.checkIsUIThread()

    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.player_rate_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int) {

    UIThread.checkIsUIThread()

    val item = this.rates[position]
    holder.text.text = textOfRate(item)
    holder.border.visibility = INVISIBLE

    if (item == this.currentRate) {
      holder.border.visibility = VISIBLE
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
    val border: ImageView = view.findViewById(R.id.player_rate_item_view_border)

    init {
      this.border.setColorFilter(
        this@PlayerPlaybackRateAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)
    }
  }
}
