package org.librarysimplified.audiobook.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadExpired
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.views.PlayerAccessibilityEvent.PlayerAccessibilityChapterSelected
import org.slf4j.LoggerFactory
import rx.Subscription

/**
 * A table of contents fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.librarysimplified.audiobook.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerTOCFragment : Fragment() {

  private val log = LoggerFactory.getLogger(PlayerTOCFragment::class.java)

  private lateinit var listener: PlayerFragmentListenerType
  private lateinit var adapter: PlayerTOCAdapter
  private lateinit var book: PlayerAudioBookType
  private lateinit var player: PlayerType
  private lateinit var parameters: PlayerTOCFragmentParameters
  private var menuInitialized = false
  private lateinit var menuRefreshAll: MenuItem
  private lateinit var menuCancelAll: MenuItem

  private var bookSubscription: Subscription? = null
  private var playerSubscription: Subscription? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?
  ): View {

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
    this.log.debug("onCreate")
    super.onCreate(state)

    /*
     * This fragment wants an options menu.
     */

    this.setHasOptionsMenu(true)
  }

  override fun onDestroy() {
    super.onDestroy()

    this.bookSubscription?.unsubscribe()
    this.playerSubscription?.unsubscribe()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    this.parameters =
      this.arguments!!.getSerializable(parametersKey)
      as PlayerTOCFragmentParameters

    if (context is PlayerFragmentListenerType) {
      this.listener = context

      this.book = this.listener.onPlayerTOCWantsBook()
      this.player = this.listener.onPlayerWantsPlayer()

      this.adapter =
        PlayerTOCAdapter(
          context = context,
          spineElements = this.book.spine,
          onSelect = { item -> this.onTOCItemSelected(item) }
        )

      this.bookSubscription =
        this.book.spineElementDownloadStatus.subscribe(
          { status -> this.onSpineElementStatusChanged(status) },
          { error -> this.onSpineElementStatusError(error) },
          { }
        )

      this.playerSubscription =
        this.player.events.subscribe(
          { event -> this.onPlayerEvent(event) },
          { error -> this.onPlayerError(error) },
          { }
        )
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
          .toString()
      )
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    this.log.debug("onCreateOptionsMenu")
    super.onCreateOptionsMenu(menu, inflater)

    inflater.inflate(R.menu.player_toc_menu, menu)

    this.menuRefreshAll = menu.findItem(R.id.player_toc_menu_refresh_all)
    this.menuCancelAll = menu.findItem(R.id.player_toc_menu_stop_all)
    this.menuInitialized = true
    this.menusConfigureVisibility()
  }

  private fun menusConfigureVisibility() {
    UIThread.checkIsUIThread()

    if (this.menuInitialized) {
      val refreshVisibleThen = this.menuRefreshAll.isVisible
      val cancelVisibleThen = this.menuCancelAll.isVisible

      val refreshVisibleNow =
        this.book.spine.any { item -> isRefreshable(item) }
      val cancelVisibleNow =
        this.book.spine.any { item -> isCancellable(item) }

      if (refreshVisibleNow != refreshVisibleThen || cancelVisibleNow != cancelVisibleThen) {
        this.menuRefreshAll.isVisible = refreshVisibleNow
        this.menuCancelAll.isVisible = cancelVisibleNow
        this.activity!!.invalidateOptionsMenu()
      }
    }
  }

  private fun isCancellable(item: PlayerSpineElementType): Boolean {
    return when (item.downloadStatus) {
      is PlayerSpineElementDownloadExpired -> false
      is PlayerSpineElementDownloadFailed -> false
      is PlayerSpineElementNotDownloaded -> false
      is PlayerSpineElementDownloading -> true
      is PlayerSpineElementDownloaded -> false
    }
  }

  private fun isRefreshable(item: PlayerSpineElementType): Boolean {
    return when (item.downloadStatus) {
      is PlayerSpineElementDownloadExpired -> false
      is PlayerSpineElementDownloadFailed -> true
      is PlayerSpineElementNotDownloaded -> true
      is PlayerSpineElementDownloading -> false
      is PlayerSpineElementDownloaded -> false
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId
    return when (id) {
      R.id.player_toc_menu_refresh_all -> {
        this.onMenuRefreshAllSelected()
        true
      }
      R.id.player_toc_menu_stop_all -> {
        this.onMenuStopAllSelected()
        true
      }
      else -> {
        this.log.debug("unrecognized menu item: {}", id)
        false
      }
    }
  }

  private fun onMenuStopAllSelected() {
    this.log.debug("onMenuStopAllSelected")

    val dialog =
      AlertDialog.Builder(this.context)
        .setCancelable(true)
        .setMessage(R.string.audiobook_player_toc_menu_stop_all_confirm)
        .setPositiveButton(
          R.string.audiobook_part_download_stop,
          { _: DialogInterface, _: Int -> onMenuStopAllSelectedConfirmed() }
        )
        .setNegativeButton(
          R.string.audiobook_part_download_continue,
          { _: DialogInterface, _: Int -> }
        )
        .create()
    dialog.show()
  }

  private fun onMenuStopAllSelectedConfirmed() {
    this.log.debug("onMenuStopAllSelectedConfirmed")
    this.book.wholeBookDownloadTask.cancel()
  }

  private fun onMenuRefreshAllSelected() {
    this.log.debug("onMenuRefreshAllSelected")
    this.book.wholeBookDownloadTask.fetch()
  }

  private fun onTOCItemSelected(item: PlayerSpineElementType) {
    this.log.debug("onTOCItemSelected: ", item.index)

    try {
      this.listener.onPlayerAccessibilityEvent(
        PlayerAccessibilityChapterSelected(
          this.context!!.getString(R.string.audiobook_accessibility_toc_selected, item.index + 1)
        )
      )
    } catch (ex: Exception) {
      this.log.debug("ignored exception in event handler: ", ex)
    }

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

      is PlayerSpineElementDownloadExpired ->
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
      is PlayerEvent.PlayerEventError -> Unit
      PlayerEvent.PlayerEventManifestUpdated -> Unit
    }
  }

  private fun onPlayerSpineElement(index: Int) {
    UIThread.runOnUIThread(
      Runnable {
        this.adapter.setCurrentSpineElement(index)
      }
    )
  }

  private fun onSpineElementStatusError(error: Throwable?) {
    this.log.error("onSpineElementStatusError: ", error)
  }

  private fun onSpineElementStatusChanged(status: PlayerSpineElementDownloadStatus) {
    UIThread.runOnUIThread(
      Runnable {
        val spineElement = status.spineElement
        this.adapter.notifyItemChanged(spineElement.index)
        this.menusConfigureVisibility()
      }
    )
  }

  companion object {

    private val parametersKey =
      "org.librarysimplified.audiobook.views.PlayerTOCFragment.parameters"

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
