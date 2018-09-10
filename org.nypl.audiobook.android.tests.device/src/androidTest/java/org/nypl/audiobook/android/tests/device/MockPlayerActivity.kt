package org.nypl.audiobook.android.tests.device

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.nypl.audiobook.android.mocking.MockingAudioBook
import org.nypl.audiobook.android.mocking.MockingDownloadProvider
import org.nypl.audiobook.android.mocking.MockingPlayer
import org.nypl.audiobook.android.mocking.MockingSleepTimer
import org.nypl.audiobook.android.views.PlayerFragment
import org.nypl.audiobook.android.views.PlayerFragmentListenerType
import org.nypl.audiobook.android.views.PlayerFragmentParameters
import org.nypl.audiobook.android.views.PlayerPlaybackRateFragment
import org.nypl.audiobook.android.views.PlayerSleepTimerFragment
import org.nypl.audiobook.android.views.PlayerTOCFragment
import org.nypl.audiobook.android.views.PlayerTOCFragmentParameters
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MockPlayerActivity : FragmentActivity(), PlayerFragmentListenerType {

  val timer: MockingSleepTimer = MockingSleepTimer()
  val player: MockingPlayer = MockingPlayer()

  val downloadExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4))
  val downloadStatusExecutor: ExecutorService =
    Executors.newFixedThreadPool(1)

  val downloadProvider: PlayerDownloadProviderType =
    MockingDownloadProvider(executorService = downloadExecutor)

  val book: MockingAudioBook =
    MockingAudioBook(
      id = PlayerBookID.transform("abc"),
      player = this.player,
      downloadStatusExecutor = this.downloadStatusExecutor,
      downloadProvider = this.downloadProvider)

  lateinit var playerFragment: PlayerFragment

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    for (i in 0..100) {
      val e = this.book.createSpineElement(
        "id$i",
        "P$i",
        Duration.standardSeconds(20))
    }

    this.setContentView(R.layout.mocking_player_activity)

    this.playerFragment = PlayerFragment.newInstance(PlayerFragmentParameters())

    this.supportFragmentManager
      .beginTransaction()
      .replace(R.id.mocking_player_fragment_holder, this.playerFragment, "PLAYER")
      .commit()
  }

  override fun onPlayerWantsPlayer(): PlayerType {
    return this.player
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
      .replace(R.id.mocking_player_fragment_holder, fragment, "PLAYER_TOC")
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
    Handler(Looper.getMainLooper()).post {
      val fragment = PlayerPlaybackRateFragment.newInstance()
      fragment.show(this.supportFragmentManager, "PLAYER_RATE")
    }
  }

  override fun onPlayerSleepTimerShouldOpen() {
    Handler(Looper.getMainLooper()).post {
      val fragment = PlayerSleepTimerFragment.newInstance()
      fragment.show(this.supportFragmentManager, "PLAYER_SLEEP_TIMER")
    }
  }
}
