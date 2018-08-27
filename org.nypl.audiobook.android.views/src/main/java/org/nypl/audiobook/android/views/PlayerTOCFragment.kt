package org.nypl.audiobook.android.views

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import org.slf4j.LoggerFactory
import rx.Subscription

/**
 * A table of contents fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.nypl.audiobook.android.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerTOCFragment : Fragment() {

  private val log = LoggerFactory.getLogger(PlayerTOCFragment::class.java)

  private lateinit var listener: PlayerFragmentListenerType
  private lateinit var adapter: PlayerTOCAdapter
  private lateinit var book: PlayerAudioBookType
  private lateinit var player: PlayerType
  private lateinit var parameters: PlayerTOCFragmentParameters

  private var bookSubscription: Subscription? = null
  private var playerSubscription: Subscription? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {

    val view: RecyclerView =
      inflater.inflate(R.layout.player_toc_view, container, false) as RecyclerView

    view.layoutManager = LinearLayoutManager(view.context)
    view.setHasFixedSize(true)
    view.adapter = this.adapter

    /*
     * https://jira.nypl.org/browse/SIMPLY-1152
     *
     * By default, the RecyclerView will animate cells each time the underlying adapter is
     * notified that a cell has changed. This appears to be a completely broken "feature", because
     * all it actually does is screw up list rendering to the point that that cells bounce and
     * jiggle about more or less at random when the list scrolls. This gruesome line of code
     * turns the animation off.
     */

    (view.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    return view
  }

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.parameters =
      this.arguments!!.getSerializable(parametersKey)
        as PlayerTOCFragmentParameters
  }

  override fun onDestroy() {
    super.onDestroy()

    this.bookSubscription?.unsubscribe()
    this.playerSubscription?.unsubscribe()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is PlayerFragmentListenerType) {
      this.listener = context

      this.book = this.listener.onPlayerTOCWantsBook()
      this.player = this.listener.onPlayerWantsPlayer()

      this.adapter =
        PlayerTOCAdapter(
          context = context,
          spineElements = this.book.spine,
          onSelect = { item -> this.onTOCItemSelected(item) })

      this.bookSubscription =
        this.book.spineElementDownloadStatus.subscribe(
          { status -> this.onSpineElementStatusChanged(status) },
          { error -> this.onSpineElementStatusError(error) },
          { })

      this.playerSubscription =
        this.player.events.subscribe(
          { event -> this.onPlayerEvent(event) },
          { error -> this.onPlayerError(error) },
          { })

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

  private fun onTOCItemSelected(item: PlayerSpineElementType) {
    this.log.debug("onTOCItemSelected: ", item.index)

    return when (item.downloadStatus) {
      is PlayerSpineElementNotDownloaded ->
        if (this.book.supportsStreaming) {
          this.playItemAndClose(item)
        } else {

        }

      is PlayerSpineElementDownloading ->
        if (this.book.supportsStreaming) {
          this.playItemAndClose(item)
        } else {

        }

      is PlayerSpineElementDownloaded ->
        this.playItemAndClose(item)

      is PlayerSpineElementDownloadFailed ->
        if (this.book.supportsStreaming) {
          this.playItemAndClose(item)
        } else {

        }
    }
  }

  private fun playItemAndClose(item: PlayerSpineElementType) {
    this.player.playAtLocation(item.position)
    this.closeTOC()
  }

  private fun closeTOC() {
    this.listener.onPlayerTOCWantsClose()
  }

  private fun onPlayerError(error: Throwable) {
    this.log.error("onPlayerError: ", error)
  }

  private fun onPlayerEvent(event: PlayerEvent) {
    return when (event) {
      is PlayerEvent.PlayerEventPlaybackRateChanged -> Unit
      is PlayerEvent.PlayerEventWithSpineElement ->
        this.onPlayerSpineElement(event.spineElement.index)
    }
  }

  private fun onPlayerSpineElement(index: Int) {
    UIThread.runOnUIThread(Runnable {
      this.adapter.setCurrentSpineElement(index)
    })
  }

  private fun onSpineElementStatusError(error: Throwable?) {
    this.log.error("onSpineElementStatusError: ", error)
  }

  private fun onSpineElementStatusChanged(status: PlayerSpineElementDownloadStatus) {
    UIThread.runOnUIThread(Runnable {
      val spineElement = status.spineElement
      this.adapter.notifyItemChanged(spineElement.index)
    })
  }

  companion object {

    private val parametersKey =
      "org.nypl.audiobook.android.views.PlayerTOCFragment.parameters"

    @JvmStatic
    fun newInstance(parameters: PlayerTOCFragmentParameters): PlayerTOCFragment {
      val args = Bundle()
      args.putSerializable(parametersKey, parameters)
      val fragment = PlayerTOCFragment()
      fragment.arguments = args
      return fragment
    }
  }
}
