package org.nypl.audiobook.android.tests.sandbox

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.Button
import android.widget.ImageView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.mocking.MockingAudioBook
import org.nypl.audiobook.android.mocking.MockingDownloadProvider
import org.nypl.audiobook.android.mocking.MockingPlayer
import org.nypl.audiobook.android.mocking.MockingSleepTimer
import org.nypl.audiobook.android.views.PlayerCancelAllDownloadsDialog
import org.nypl.audiobook.android.views.PlayerFragment
import org.nypl.audiobook.android.views.PlayerFragmentListenerType
import org.nypl.audiobook.android.views.PlayerFragmentParameters
import org.nypl.audiobook.android.views.PlayerTOCFragment
import org.nypl.audiobook.android.views.PlayerTOCFragmentParameters
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SandboxPlayerActivity : FragmentActivity(), PlayerFragmentListenerType {

  private val timer: MockingSleepTimer = MockingSleepTimer()

  private val downloadExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  private val downloadStatusExecutor: ExecutorService =
    Executors.newFixedThreadPool(1)

  private val downloadProvider: PlayerDownloadProviderType =
    MockingDownloadProvider(
      executorService = this.downloadExecutor,
      shouldFail = { request ->
        request.uri.toString().endsWith("0") || request.uri.toString().endsWith("5")
      })

  private val lorem = SandboxLoremIpsum.create()

  private val book: MockingAudioBook =
    MockingAudioBook(
      id = PlayerBookID.transform("abc"),
      players = { book -> MockingPlayer(book) },
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadProvider = this.downloadProvider)

  private val player: MockingPlayer = this.book.createPlayer()

  private lateinit var playerFragment: PlayerFragment

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.book.supportsStreaming = false

    for (i in 0..100) {
      val e = this.book.createSpineElement(
        "id$i",
        "Chapter $i: " + this.lorem.lines[i % this.lorem.lines.size],
        Duration.standardSeconds(20))

      if (!i.toString().endsWith("3")) {
        // e.downloadTask.fetch()
      }
    }

    this.setContentView(R.layout.example_player_activity)

    this.playerFragment =
      PlayerFragment.newInstance(PlayerFragmentParameters(
        primaryColor = Color.parseColor("#af1a16")))

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.example_player_fragment_holder, this.playerFragment, "PLAYER")
      .commit()
  }

  override fun onPlayerWantsPlayer(): PlayerType {
    return this.player
  }

  override fun onPlayerWantsCoverImage(view: ImageView) {

    /*
     * Create a controls menu that pops up when long-clicking on the cover image.
     */

    view.setOnLongClickListener { _ ->
      val dialogView = this.layoutInflater.inflate(R.layout.controls_dialog, null)

      /*
       * A button that triggers a player error.
       */

      val triggerError = dialogView.findViewById<Button>(R.id.controls_error)
      triggerError.setOnClickListener { _ ->
        this.player.error(IllegalStateException("Serious problem occurred."), 1138)
      }

      val dialog =
        AlertDialog.Builder(this)
          .setTitle("Controls")
          .setView(dialogView)
          .create()

      dialog.show()
      true
    }
  }

  override fun onPlayerTOCWantsCancelAllDownloads(confirm: () -> Unit) {
    PlayerCancelAllDownloadsDialog.create(
      activity = this,
      theme = packageManager.getActivityInfo(componentName, 0).themeResource,
      confirm = confirm)
      .show()
  }

  override fun onPlayerWantsTitle(): String {
    return "Any Title"
  }

  override fun onPlayerWantsAuthor(): String {
    return "Any Author"
  }

  override fun onPlayerWantsSleepTimer(): PlayerSleepTimerType {
    return this.timer
  }

  override fun onPlayerTOCShouldOpen() {
    val fragment =
      PlayerTOCFragment.newInstance(PlayerTOCFragmentParameters(
        primaryColor = Color.parseColor("#af1a16")))

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.example_player_fragment_holder, fragment, "PLAYER_TOC")
      .addToBackStack(null)
      .commit()
  }

  override fun onPlayerTOCWantsBook(): PlayerAudioBookType {
    return this.book
  }

  override fun onPlayerTOCWantsClose() {
    this.supportFragmentManager.popBackStack()
  }

  override fun onPlayerPlaybackRateShouldOpen() {

  }

  override fun onPlayerSleepTimerShouldOpen() {

  }
}
