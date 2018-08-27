package org.nypl.audiobook.android.tests.sandbox

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.widget.ImageView
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerBookID
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.views.PlayerFragment
import org.nypl.audiobook.android.views.PlayerFragmentListenerType
import org.nypl.audiobook.android.views.PlayerFragmentParameters
import org.nypl.audiobook.android.views.PlayerTOCFragment
import org.nypl.audiobook.android.views.PlayerTOCFragmentParameters
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TOCExample : FragmentActivity(), PlayerFragmentListenerType {

  private val timer: NullSleepTimer = NullSleepTimer()
  private val player: NullPlayer = NullPlayer()

  private val downloadExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  private val downloadStatusExecutor: ExecutorService =
    Executors.newFixedThreadPool(1)

  private val downloadProvider: PlayerDownloadProviderType =
    NullDownloadProvider(executorService = downloadExecutor)

  private val book: NullAudioBook =
    NullAudioBook(
      id = PlayerBookID.transform("abc"),
      player =  this.player,
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadProvider = this.downloadProvider)

  private lateinit var playerFragment: PlayerFragment

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    for (i in 0 .. 100) {
      val e = this.book.createSpineElement(
        "id$i",
        "P$i",
        Duration.standardSeconds(20))
      e.downloadTask.fetch()
    }

    this.setContentView(R.layout.example_player_activity)

    this.playerFragment = PlayerFragment.newInstance(PlayerFragmentParameters())

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.example_player_fragment_holder, this.playerFragment, "PLAYER")
      .commit()
  }

  override fun onPlayerWantsPlayer(): PlayerType {
    return NullPlayer()
  }

  override fun onPlayerWantsCoverImage(view: ImageView) {

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

  }

  override fun onPlayerSleepTimerShouldOpen() {

  }
}
