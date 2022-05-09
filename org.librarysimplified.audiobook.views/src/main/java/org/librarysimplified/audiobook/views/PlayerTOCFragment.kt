package org.librarysimplified.audiobook.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.reactivex.disposables.Disposable
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerEvent
import org.librarysimplified.audiobook.api.PlayerPosition
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadExpired
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.librarysimplified.audiobook.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.librarysimplified.audiobook.api.PlayerSpineElementType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.views.PlayerAccessibilityEvent.PlayerAccessibilityChapterSelected
import org.slf4j.LoggerFactory

/**
 * A table of contents fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.librarysimplified.audiobook.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerTOCFragment(
  private val listener: PlayerFragmentListenerType,
  private val book: PlayerAudioBookType,
  private val player: PlayerType
) : Fragment() {

  private val log = LoggerFactory.getLogger(PlayerTOCFragment::class.java)

  private lateinit var adapter: PlayerTOCAdapter

  private var bookSubscription: Disposable? = null
  private var playerSubscription: Disposable? = null

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

    this.bookSubscription?.dispose()
    this.playerSubscription?.dispose()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    this.adapter =
      PlayerTOCAdapter(
        context = context,
        spineElements = this.book.spine,
        onSelect = { item -> this.onTOCItemSelected(item) }
      )

    this.playerSubscription =
      this.player.events.subscribe(
        { event -> this.onPlayerEvent(event) },
        { error -> this.onPlayerError(error) },
        { }
      )
  }

  private fun onTOCItemSelected(item: PlayerSpineElementType) {
    this.log.debug("onTOCItemSelected: ", item.index)

    try {
      this.listener.onPlayerAccessibilityEvent(
        PlayerAccessibilityChapterSelected(
          this.requireContext()
            .getString(R.string.audiobook_accessibility_toc_selected, item.index + 1)
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
    val position = PlayerPosition(
      title = item.title,
      chapter = item.index,
      part = 0,
      offsetMilliseconds = 0
    )
    this.player.playAtLocation(position)
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
}
