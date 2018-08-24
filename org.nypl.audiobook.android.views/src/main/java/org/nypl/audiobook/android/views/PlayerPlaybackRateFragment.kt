package org.nypl.audiobook.android.views

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory

/**
 * A playback rate configuration fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.nypl.audiobook.android.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerPlaybackRateFragment : DialogFragment() {

  private val log = LoggerFactory.getLogger(PlayerPlaybackRateFragment::class.java)
  private lateinit var listener: PlayerFragmentListenerType
  private lateinit var adapter: PlayerPlaybackRateAdapter
  private lateinit var player: PlayerType

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val view: RecyclerView =
      inflater.inflate(R.layout.player_rate_view, container, false) as RecyclerView

    this.dialog.setTitle(R.string.audiobook_player_menu_playback_rate_title)

    view.layoutManager = LinearLayoutManager(view.context)
    view.setHasFixedSize(true)
    view.adapter = this.adapter

    return view
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is PlayerFragmentListenerType) {
      this.listener = context

      this.player = this.listener.onPlayerWantsPlayer()

      this.adapter =
        PlayerPlaybackRateAdapter(
          rates = PlayerPlaybackRate.values().toList(),
          onSelect = { item -> this.onPlaybackRateSelected(item) })

      this.adapter.setCurrentPlaybackRate(this.player.playbackRate)

    } else {
      throw ClassCastException(
        StringBuilder(64)
          .append("The activity hosting this fragment must implement one or more listener interfaces.\n")
          .append("  Activity: ")
          .append(context::class.java.canonicalName)
          .append('\n')
          .append("  Required interface: ")
          .append(PlayerFragmentListenerType::class.java.canonicalName)
          .append('\n')
          .toString())
    }
  }

  private fun onPlaybackRateSelected(item: PlayerPlaybackRate) {
    this.log.debug("onPlaybackRateSelected: {}", item)

    this.adapter.setCurrentPlaybackRate(item)
    this.player.playbackRate = item
  }

  companion object {
    @JvmStatic
    fun newInstance(): PlayerPlaybackRateFragment {
      return PlayerPlaybackRateFragment()
    }
  }
}
