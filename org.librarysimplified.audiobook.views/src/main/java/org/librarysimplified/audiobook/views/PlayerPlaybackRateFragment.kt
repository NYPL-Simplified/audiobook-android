package org.librarysimplified.audiobook.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.views.PlayerAccessibilityEvent.PlayerAccessibilityPlaybackRateChanged
import org.slf4j.LoggerFactory

/**
 * A playback rate configuration fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.librarysimplified.audiobook.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerPlaybackRateFragment(
  private val listener: PlayerFragmentListenerType,
  private val player: PlayerType
) : DialogFragment() {

  private val log = LoggerFactory.getLogger(PlayerPlaybackRateFragment::class.java)
  private lateinit var adapter: PlayerPlaybackRateAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?
  ): View {

    val view: RecyclerView =
      inflater.inflate(R.layout.player_rate_view, container, false) as RecyclerView

    this.dialog?.setTitle(R.string.audiobook_player_menu_playback_rate_title)

    view.layoutManager = LinearLayoutManager(view.context)
    view.setHasFixedSize(true)
    view.adapter = this.adapter

    return view
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    this.adapter =
      PlayerPlaybackRateAdapter(
        resources = this.resources,
        rates = PlayerPlaybackRate.values().toList(),
        onSelect = { item -> this.onPlaybackRateSelected(item) }
      )

    this.adapter.setCurrentPlaybackRate(this.player.playbackRate)
  }

  private fun onPlaybackRateSelected(item: PlayerPlaybackRate) {
    this.log.debug("onPlaybackRateSelected: {}", item)

    try {
      this.listener.onPlayerAccessibilityEvent(
        PlayerAccessibilityPlaybackRateChanged(
          PlayerPlaybackRateAdapter.hasBeenSetToContentDescriptionOfRate(resources, item)
        )
      )
    } catch (ex: Exception) {
      this.log.debug("ignored exception in handler: ", ex)
    }

    this.adapter.setCurrentPlaybackRate(item)
    this.player.playbackRate = item
    this.dismiss()
  }
}
