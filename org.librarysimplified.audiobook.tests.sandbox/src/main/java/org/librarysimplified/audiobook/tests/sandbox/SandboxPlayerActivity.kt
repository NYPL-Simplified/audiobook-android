package org.librarysimplified.audiobook.tests.sandbox

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import org.librarysimplified.audiobook.api.PlayerAudioBookType
import org.librarysimplified.audiobook.api.PlayerBookID
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerSleepTimerType
import org.librarysimplified.audiobook.api.PlayerType
import org.librarysimplified.audiobook.mocking.MockingAudioBook
import org.librarysimplified.audiobook.mocking.MockingDownloadProvider
import org.librarysimplified.audiobook.mocking.MockingPlayer
import org.librarysimplified.audiobook.mocking.MockingSleepTimer
import org.librarysimplified.audiobook.views.PlayerAccessibilityEvent
import org.librarysimplified.audiobook.views.PlayerFragment
import org.librarysimplified.audiobook.views.PlayerFragmentListenerType
import org.librarysimplified.audiobook.views.PlayerFragmentParameters
import org.librarysimplified.audiobook.views.PlayerPlaybackRateFragment
import org.librarysimplified.audiobook.views.PlayerSleepTimerFragment
import org.librarysimplified.audiobook.views.PlayerTOCFragment
import org.librarysimplified.audiobook.views.PlayerTOCFragmentParameters
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class SandboxPlayerActivity : AppCompatActivity(), PlayerFragmentListenerType {

  private val timer: MockingSleepTimer = MockingSleepTimer()

  private val downloadExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  private val downloadStatusExecutor: ExecutorService =
    Executors.newFixedThreadPool(1)
  private val scheduledExecutor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor()

  private val downloadProvider: PlayerDownloadProviderType =
    MockingDownloadProvider(
      executorService = this.downloadExecutor,
      shouldFail = { request ->
        request.uri.toString().endsWith("0") || request.uri.toString().endsWith("5")
      }
    )

  private val lorem = SandboxLoremIpsum.create()

  private val book: MockingAudioBook =
    MockingAudioBook(
      id = PlayerBookID.transform("abc"),
      players = { book -> MockingPlayer(book) },
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadProvider = this.downloadProvider
    )

  private val player: MockingPlayer = this.book.createPlayer()

  private lateinit var playerFragment: PlayerFragment

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.setTheme(R.style.AudioBooksWithActionBar)
    this.book.supportsStreaming = false

    for (i in 1..100) {
      val e = this.book.createSpineElement(
        "id$i",
        this.lorem.lines[i % this.lorem.lines.size],
        Duration.standardSeconds(20)
      )

      if (!i.toString().endsWith("3")) {
        e.downloadTask().fetch()
      }
    }

    this.setContentView(R.layout.example_player_activity)

    this.playerFragment =
      PlayerFragment.newInstance(PlayerFragmentParameters())

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

    view.setOnLongClickListener {
      val dialogView = this.layoutInflater.inflate(R.layout.controls_dialog, null)

      /*
       * A button that triggers a player error.
       */

      val triggerError = dialogView.findViewById<Button>(R.id.controls_error)
      triggerError.setOnClickListener {
        this.player.error(IllegalStateException("Serious problem occurred."), 1138)
      }

      /*
       * A button that triggers a player error.
       */

      val triggerBuffering = dialogView.findViewById<Button>(R.id.controls_buffering)
      triggerBuffering.setOnClickListener {
        this.player.buffering()
      }

      /*
       * A button that enables streaming.
       */

      val triggerStream = dialogView.findViewById<Button>(R.id.controls_set_streamable)
      triggerStream.setOnClickListener {
        this.book.supportsStreaming = true
      }

      /*
       * A button that disables streaming.
       */

      val triggerStreamOff = dialogView.findViewById<Button>(R.id.controls_set_not_streamable)
      triggerStreamOff.setOnClickListener {
        this.book.supportsStreaming = false
      }

      /*
       * A button that disables download tasks.
       */

      val triggerTasksOff = dialogView.findViewById<Button>(R.id.controls_set_download_tasks_unsupported)
      triggerTasksOff.setOnClickListener {
        this.book.spineItems.forEach { item -> item.downloadTasksAreSupported = false }
      }

      /*
       * A button that enables download tasks.
       */

      val triggerTasksOn = dialogView.findViewById<Button>(R.id.controls_set_download_tasks_supported)
      triggerTasksOn.setOnClickListener {
        this.book.spineItems.forEach { item -> item.downloadTasksAreSupported = true }
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
      PlayerTOCFragment.newInstance(PlayerTOCFragmentParameters())

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

    /*
     * The player fragment wants us to open the playback rate selection dialog.
     */

    runOnUIThread(
      Runnable {
        val fragment =
          PlayerPlaybackRateFragment.newInstance(PlayerFragmentParameters())
        fragment.show(this.supportFragmentManager, "PLAYER_RATE")
      }
    )
  }

  private fun runOnUIThread(r: Runnable) {
    val looper = Looper.getMainLooper()
    val h = Handler(looper)
    h.post(r)
  }

  override fun onPlayerSleepTimerShouldOpen() {

    /*
     * The player fragment wants us to open the sleep timer.
     */

    runOnUIThread(
      Runnable {
        val fragment =
          PlayerSleepTimerFragment.newInstance(PlayerFragmentParameters())
        fragment.show(this.supportFragmentManager, "PLAYER_SLEEP_TIMER")
      }
    )
  }

  override fun onPlayerWantsScheduledExecutor(): ScheduledExecutorService {
    return this.scheduledExecutor
  }

  override fun onPlayerAccessibilityEvent(event: PlayerAccessibilityEvent) {
    runOnUIThread(
      Runnable {
        val toast = Toast.makeText(this.applicationContext, event.message, Toast.LENGTH_SHORT)
        toast.show()
      }
    )
  }
}
