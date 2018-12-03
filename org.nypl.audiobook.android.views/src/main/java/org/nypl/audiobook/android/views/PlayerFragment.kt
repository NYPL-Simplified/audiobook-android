package org.nypl.audiobook.android.views

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import org.joda.time.Duration
import org.nypl.audiobook.android.api.PlayerAudioBookType
import org.nypl.audiobook.android.api.PlayerEvent
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventError
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventPlaybackRateChanged
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterCompleted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventChapterWaiting
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackBuffering
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackPaused
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackProgressUpdate
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStarted
import org.nypl.audiobook.android.api.PlayerEvent.PlayerEventWithSpineElement.PlayerEventPlaybackStopped
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerCancelled
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerFinished
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerRunning
import org.nypl.audiobook.android.api.PlayerSleepTimerEvent.PlayerSleepTimerStopped
import org.nypl.audiobook.android.api.PlayerSleepTimerType
import org.nypl.audiobook.android.api.PlayerSpineElementType
import org.nypl.audiobook.android.api.PlayerType
import org.nypl.audiobook.android.views.PlayerAccessibilityEvent.PlayerAccessibilityErrorOccurred
import org.nypl.audiobook.android.views.PlayerAccessibilityEvent.PlayerAccessibilityIsBuffering
import org.nypl.audiobook.android.views.PlayerAccessibilityEvent.PlayerAccessibilityIsWaitingForChapter
import org.slf4j.LoggerFactory
import rx.Subscription
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A player fragment.
 *
 * New instances MUST be created with {@link #newInstance()} rather than calling the constructor
 * directly. The public constructor only exists because the Android API requires it.
 *
 * Activities hosting this fragment MUST implement the {@link org.nypl.audiobook.android.views.PlayerFragmentListenerType}
 * interface. An exception will be raised if this is not the case.
 */

class PlayerFragment : android.support.v4.app.Fragment() {

  companion object {

    const val parametersKey = "org.nypl.audiobook.android.views.PlayerFragment.parameters"

    @JvmStatic
    fun newInstance(parameters: PlayerFragmentParameters): PlayerFragment {
      val args = Bundle()
      args.putSerializable(this.parametersKey, parameters)
      val fragment = PlayerFragment()
      fragment.arguments = args
      return fragment
    }
  }

  private lateinit var listener: PlayerFragmentListenerType
  private lateinit var player: PlayerType
  private lateinit var book: PlayerAudioBookType
  private lateinit var executor: ScheduledExecutorService
  private lateinit var sleepTimer: PlayerSleepTimerType
  private lateinit var coverView: ImageView
  private lateinit var playerTitleView: TextView
  private lateinit var playerAuthorView: TextView
  private lateinit var playPauseButton: ImageView
  private lateinit var playerSkipForwardButton: ImageView
  private lateinit var playerSkipBackwardButton: ImageView
  private var playerPositionDragging: Boolean = false
  private var playerBufferingStillOngoing: Boolean = false
  private var playerBufferingTask: ScheduledFuture<*>? = null
  private lateinit var playerPosition: SeekBar
  private lateinit var playerTimeCurrent: TextView
  private lateinit var playerTimeMaximum: TextView
  private lateinit var playerSpineElement: TextView
  private lateinit var playerWaiting: TextView
  private lateinit var menuPlaybackRate: MenuItem
  private lateinit var menuPlaybackRateText: TextView
  private lateinit var menuSleep: MenuItem
  private lateinit var menuSleepText: TextView
  private lateinit var menuSleepEndOfChapter: ImageView
  private lateinit var menuTOC: MenuItem
  private lateinit var parameters: PlayerFragmentParameters
  private lateinit var timeStrings: PlayerTimeStrings.SpokenTranslations

  private var playerPositionCurrentSpine: PlayerSpineElementType? = null
  private var playerPositionCurrentOffset: Long = 0L
  private var playerEventSubscription: Subscription? = null
  private var playerSleepTimerEventSubscription: Subscription? = null

  private val log = LoggerFactory.getLogger(PlayerFragment::class.java)

  override fun onCreate(state: Bundle?) {
    this.log.debug("onCreate")

    super.onCreate(state)

    this.parameters =
      this.arguments!!.getSerializable(parametersKey)
        as PlayerFragmentParameters
    this.timeStrings =
      PlayerTimeStrings.SpokenTranslations.createFromResources(this.resources)

    /*
     * This fragment wants an options menu.
     */

    this.setHasOptionsMenu(true)
  }

  override fun onAttach(context: Context) {
    this.log.debug("onAttach")
    super.onAttach(context)

    if (context is PlayerFragmentListenerType) {
      this.listener = context
      this.player = this.listener.onPlayerWantsPlayer()
      this.book = this.listener.onPlayerTOCWantsBook()
      this.sleepTimer = this.listener.onPlayerWantsSleepTimer()
      this.executor = this.listener.onPlayerWantsScheduledExecutor()
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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    this.log.debug("onCreateOptionsMenu")

    super.onCreateOptionsMenu(menu, inflater)

    inflater.inflate(R.menu.player_menu, menu)

    this.menuPlaybackRate = menu.findItem(R.id.player_menu_playback_rate)
    this.menuPlaybackRate.actionView.setOnClickListener { this.onMenuPlaybackRateSelected() }
    this.menuPlaybackRate.actionView.contentDescription =
      this.playbackRateContentDescription()
    this.menuPlaybackRateText =
      this.menuPlaybackRate.actionView.findViewById(R.id.player_menu_playback_rate_text)
    this.menuPlaybackRateText.text =
      PlayerPlaybackRateAdapter.textOfRate(this.player.playbackRate)

    /*
     * On API versions older than 23, playback rate changes will have no effect. There is no
     * point showing the menu.
     */

    if (Build.VERSION.SDK_INT < 23) {
      this.menuPlaybackRate.setVisible(false)
    }

    this.menuSleep = menu.findItem(R.id.player_menu_sleep)
    this.menuSleep.actionView.setOnClickListener { this.onMenuSleepSelected() }
    this.menuSleep.actionView.contentDescription = this.sleepTimerContentDescriptionSetUp()

    this.menuSleepText = this.menuSleep.actionView.findViewById(R.id.player_menu_sleep_text)
    this.menuSleepText.text = ""
    this.menuSleepText.visibility = INVISIBLE

    this.menuSleepEndOfChapter =
      this.menuSleep.actionView.findViewById(R.id.player_menu_sleep_end_of_chapter)
    this.menuSleepEndOfChapter.visibility = INVISIBLE

    this.menuTOC = menu.findItem(R.id.player_menu_toc)
    this.menuTOC.setOnMenuItemClickListener { this.onMenuTOCSelected() }

    /*
     * Subscribe to player and timer events. We do the subscription here (as late as possible)
     * so that all of the views (including the options menu) have been created before the first
     * event is received.
     */

    this.playerEventSubscription =
      this.player.events.subscribe(
        { event -> this.onPlayerEvent(event) },
        { error -> this.onPlayerError(error) },
        { this.onPlayerEventsCompleted() })

    this.playerSleepTimerEventSubscription =
      this.sleepTimer.status.subscribe(
        { event -> this.onPlayerSleepTimerEvent(event) },
        { error -> this.onPlayerSleepTimerError(error) },
        { this.onPlayerSleepTimerEventsCompleted() })
  }

  private fun onPlayerSleepTimerEventsCompleted() {
    this.log.debug("onPlayerSleepTimerEventsCompleted")
  }

  private fun onPlayerSleepTimerError(error: Throwable) {
    this.log.error("onPlayerSleepTimerError: ", error)
  }

  private fun onPlayerSleepTimerEvent(event: PlayerSleepTimerEvent) {
    this.log.debug("onPlayerSleepTimerEvent: {}", event)

    return when (event) {
      PlayerSleepTimerStopped ->
        this.onPlayerSleepTimerEventStopped()
      is PlayerSleepTimerRunning ->
        this.onPlayerSleepTimerEventRunning(event)
      is PlayerSleepTimerCancelled ->
        this.onPlayerSleepTimerEventCancelled()
      PlayerSleepTimerFinished ->
        this.onPlayerSleepTimerEventFinished()
    }
  }

  private fun onPlayerSleepTimerEventFinished() {
    this.onPressedPause()

    UIThread.runOnUIThread(Runnable {
      this.menuSleepText.text = ""
      this.menuSleep.actionView.contentDescription = this.sleepTimerContentDescriptionSetUp()
      this.menuSleepText.visibility = INVISIBLE
      this.menuSleepEndOfChapter.visibility = INVISIBLE
    })
  }

  private fun onPlayerSleepTimerEventCancelled() {
    UIThread.runOnUIThread(Runnable {
      this.menuSleepText.text = ""
      this.menuSleep.actionView.contentDescription = this.sleepTimerContentDescriptionSetUp()
      this.menuSleepText.visibility = INVISIBLE
      this.menuSleepEndOfChapter.visibility = INVISIBLE
    })
  }

  private fun onPlayerSleepTimerEventRunning(event: PlayerSleepTimerRunning) {
    UIThread.runOnUIThread(Runnable {
      val remaining = event.remaining
      if (remaining != null) {
        this.menuSleep.actionView.contentDescription =
          this.sleepTimerContentDescriptionForTime(remaining)
        this.menuSleepText.text =
          PlayerTimeStrings.minuteSecondTextFromDuration(remaining)
        this.menuSleepEndOfChapter.visibility = INVISIBLE
      } else {
        this.menuSleep.actionView.contentDescription =
          this.sleepTimerContentDescriptionEndOfChapter()
        this.menuSleepText.text = ""
        this.menuSleepEndOfChapter.visibility = VISIBLE
      }

      this.menuSleepText.visibility = VISIBLE
    })
  }

  private fun sleepTimerContentDescriptionEndOfChapter(): String {
    val builder = java.lang.StringBuilder(128)
    builder.append(this.resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_icon))
    builder.append(". ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_sleep_timer_currently))
    builder.append(" ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_sleep_timer_description_end_of_chapter))
    return builder.toString()
  }

  private fun sleepTimerContentDescriptionForTime(remaining: Duration): String {
    val builder = java.lang.StringBuilder(128)
    builder.append(this.resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_icon))
    builder.append(". ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_sleep_timer_currently))
    builder.append(" ")
    builder.append(PlayerTimeStrings.minuteSecondSpokenFromDuration(this.timeStrings, remaining))
    return builder.toString()
  }

  private fun sleepTimerContentDescriptionSetUp(): String {
    val builder = java.lang.StringBuilder(128)
    builder.append(this.resources.getString(R.string.audiobook_accessibility_menu_sleep_timer_icon))
    builder.append(". ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_sleep_timer_currently))
    builder.append(" ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_sleep_timer_description_off))
    return builder.toString()
  }

  private fun playbackRateContentDescription(): String {
    val builder = java.lang.StringBuilder(128)
    builder.append(this.resources.getString(R.string.audiobook_accessibility_menu_playback_speed_icon))
    builder.append(". ")
    builder.append(this.resources.getString(R.string.audiobook_accessibility_playback_speed_currently))
    builder.append(" ")
    builder.append(PlayerPlaybackRateAdapter.contentDescriptionOfRate(this.resources, this.player.playbackRate))
    return builder.toString()
  }

  private fun onPlayerSleepTimerEventStopped() {
    UIThread.runOnUIThread(Runnable {
      this.menuSleepText.text = ""
      this.menuSleepText.contentDescription = this.sleepTimerContentDescriptionSetUp()
      this.menuSleepText.visibility = INVISIBLE
      this.menuSleepEndOfChapter.visibility = INVISIBLE
    })
  }

  private fun onMenuTOCSelected(): Boolean {
    this.listener.onPlayerTOCShouldOpen()
    return true
  }

  private fun onMenuSleepSelected(): Boolean {
    this.listener.onPlayerSleepTimerShouldOpen()
    return true
  }

  private fun onMenuPlaybackRateSelected() {
    this.listener.onPlayerPlaybackRateShouldOpen()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    state: Bundle?): View? {
    this.log.debug("onCreateView")
    return inflater.inflate(R.layout.player_view, container, false)
  }

  override fun onDestroyView() {
    this.log.debug("onDestroyView")
    super.onDestroyView()
    this.playerEventSubscription?.unsubscribe()
    this.playerSleepTimerEventSubscription?.unsubscribe()
    this.onPlayerBufferingStopped()
  }

  override fun onViewCreated(view: View, state: Bundle?) {
    this.log.debug("onViewCreated")
    super.onViewCreated(view, state)

    this.coverView = view.findViewById(R.id.player_cover)!!

    this.playerTitleView = view.findViewById(R.id.player_title)
    this.playerAuthorView = view.findViewById(R.id.player_author)

    this.playPauseButton = view.findViewById(R.id.player_play_button)!!
    this.playPauseButton.setOnClickListener { this.onPressedPlay() }

    this.playerSkipForwardButton = view.findViewById(R.id.player_jump_forwards)
    this.playerSkipForwardButton.setOnClickListener { this.player.skipForward() }
    this.playerSkipBackwardButton = view.findViewById(R.id.player_jump_backwards)
    this.playerSkipBackwardButton.setOnClickListener { this.player.skipBack() }

    this.playerWaiting = view.findViewById(R.id.player_waiting_buffering)
    this.playerWaiting.text = ""
    this.playerWaiting.contentDescription = null

    this.playerPosition = view.findViewById(R.id.player_progress)!!
    this.playerPosition.thumbTintList = ColorStateList.valueOf(this.parameters.primaryColor)
    this.playerPosition.progressTintList = ColorStateList.valueOf(this.parameters.primaryColor)
    this.playerPosition.isEnabled = false
    this.playerPositionDragging = false
    this.playerPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        this@PlayerFragment.onProgressBarChanged(progress, fromUser)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
        this@PlayerFragment.onProgressBarDraggingStarted()
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        this@PlayerFragment.onProgressBarDraggingStopped()
      }
    })

    this.playerTimeCurrent = view.findViewById(R.id.player_time)!!
    this.playerTimeMaximum = view.findViewById(R.id.player_time_maximum)!!
    this.playerSpineElement = view.findViewById(R.id.player_spine_element)!!
    this.playerSpineElement.text = this.spineElementText(this.book.spine.first())

    this.listener.onPlayerWantsCoverImage(this.coverView)
    this.playerTitleView.text = this.listener.onPlayerWantsTitle()
    this.playerAuthorView.text = this.listener.onPlayerWantsAuthor()
  }

  private fun onProgressBarDraggingStopped() {
    this.log.debug("onProgressBarDraggingStopped")
    this.playerPositionDragging = false

    val spine = this.playerPositionCurrentSpine
    if (spine != null) {
      val target = spine.position.copy(
        offsetMilliseconds =
        TimeUnit.MILLISECONDS.convert(this.playerPosition.progress.toLong(), TimeUnit.SECONDS))
      if (player.isPlaying) {
        this.player.playAtLocation(target)
      } else {
        this.player.movePlayheadToLocation(target)
      }
    }
  }

  private fun onProgressBarDraggingStarted() {
    this.log.debug("onProgressBarDraggingStarted")
    this.playerPositionDragging = true
  }

  private fun onProgressBarChanged(progress: Int, fromUser: Boolean) {
    this.log.debug("onProgressBarChanged: {} {}", progress, fromUser)
  }

  private fun onPlayerEventsCompleted() {
    this.log.debug("onPlayerEventsCompleted")
  }

  private fun onPlayerError(error: Throwable) {
    this.log.debug("onPlayerError: ", error)
  }

  private fun onPlayerEvent(event: PlayerEvent) {
    this.log.debug("onPlayerEvent: {}", event)

    return when (event) {
      is PlayerEventPlaybackStarted ->
        this.onPlayerEventPlaybackStarted(event)
      is PlayerEventPlaybackBuffering ->
        this.onPlayerEventPlaybackBuffering(event)
      is PlayerEventChapterWaiting ->
        this.onPlayerEventChapterWaiting(event)
      is PlayerEventPlaybackProgressUpdate ->
        this.onPlayerEventPlaybackProgressUpdate(event)
      is PlayerEventChapterCompleted ->
        this.onPlayerEventChapterCompleted()
      is PlayerEventPlaybackPaused ->
        this.onPlayerEventPlaybackPaused(event)
      is PlayerEventPlaybackStopped ->
        this.onPlayerEventPlaybackStopped(event)
      is PlayerEventPlaybackRateChanged ->
        this.onPlayerEventPlaybackRateChanged(event)
      is PlayerEventError ->
        this.onPlayerEventError(event)
    }
  }

  private fun onPlayerEventError(event: PlayerEventError) {
    UIThread.runOnUIThread(Runnable {
      val text = this.getString(R.string.audiobook_player_error, event.errorCode)
      this.playerWaiting.setText(text)
      this.playerWaiting.contentDescription = null
      this.listener.onPlayerAccessibilityEvent(PlayerAccessibilityErrorOccurred(text))

      val element = event.spineElement
      if (element != null) {
        this.configureSpineElementText(element)
        this.onEventUpdateTimeRelatedUI(element, event.offsetMilliseconds)
      }
    })
  }

  private fun onPlayerEventChapterWaiting(event: PlayerEventChapterWaiting) {
    UIThread.runOnUIThread(Runnable {
      val text =
        this.getString(R.string.audiobook_player_waiting, event.spineElement.index + 1)
      this.playerWaiting.setText(text)
      this.playerWaiting.contentDescription = null
      this.listener.onPlayerAccessibilityEvent(PlayerAccessibilityIsWaitingForChapter(text))

      this.configureSpineElementText(event.spineElement)
      this.onEventUpdateTimeRelatedUI(event.spineElement, 0)
    })
  }

  private fun onPlayerEventPlaybackBuffering(event: PlayerEventPlaybackBuffering) {
    UIThread.runOnUIThread(Runnable {
      this.onPlayerBufferingStarted()
      this.configureSpineElementText(event.spineElement)
      this.onEventUpdateTimeRelatedUI(event.spineElement, event.offsetMilliseconds)
    })
  }

  private fun onPlayerEventChapterCompleted() {
    this.onPlayerBufferingStopped()

    /*
     * If the chapter is completed, and the sleep timer is running indefinitely, then
     * tell the sleep timer to complete.
     */

    val running = this.sleepTimer.isRunning
    if (running != null) {
      if (running.duration == null) {
        this.sleepTimer.finish()
      }
    }
  }

  private fun onPlayerEventPlaybackRateChanged(event: PlayerEventPlaybackRateChanged) {
    UIThread.runOnUIThread(Runnable {
      this.menuPlaybackRateText.text = PlayerPlaybackRateAdapter.textOfRate(event.rate)
      this.menuPlaybackRate.actionView.contentDescription = this.playbackRateContentDescription()
    })
  }

  private fun onPlayerEventPlaybackStopped(event: PlayerEventPlaybackStopped) {
    this.onPlayerBufferingStopped()

    UIThread.runOnUIThread(Runnable {
      this.playPauseButton.setImageResource(R.drawable.play_icon)
      this.playPauseButton.setOnClickListener { this.onPressedPlay() }
      this.playPauseButton.contentDescription = this.getString(R.string.audiobook_accessibility_play)
      this.configureSpineElementText(event.spineElement)
      this.onEventUpdateTimeRelatedUI(event.spineElement, event.offsetMilliseconds)
    })
  }

  private fun onPlayerEventPlaybackPaused(event: PlayerEventPlaybackPaused) {
    this.onPlayerBufferingStopped()

    UIThread.runOnUIThread(Runnable {
      this.playPauseButton.setImageResource(R.drawable.play_icon)
      this.playPauseButton.setOnClickListener { this.onPressedPlay() }
      this.playPauseButton.contentDescription = this.getString(R.string.audiobook_accessibility_play)
      this.configureSpineElementText(event.spineElement)
      this.onEventUpdateTimeRelatedUI(event.spineElement, event.offsetMilliseconds)
    })
  }

  private fun onPressedPlay() {
    this.player.play()
  }

  private fun onPressedPause() {
    this.player.pause()
  }

  private fun onPlayerEventPlaybackProgressUpdate(event: PlayerEventPlaybackProgressUpdate) {
    this.onPlayerBufferingStopped()

    UIThread.runOnUIThread(Runnable {
      this.playPauseButton.setImageResource(R.drawable.pause_icon)
      this.playPauseButton.setOnClickListener { this.onPressedPause() }
      this.playPauseButton.contentDescription = this.getString(R.string.audiobook_accessibility_pause)
      this.playerWaiting.text = ""
      this.playerWaiting.contentDescription = null
      this.onEventUpdateTimeRelatedUI(event.spineElement, event.offsetMilliseconds)
    })
  }

  private fun onPlayerEventPlaybackStarted(event: PlayerEventPlaybackStarted) {
    this.onPlayerBufferingStopped()

    UIThread.runOnUIThread(Runnable {
      this.playPauseButton.setImageResource(R.drawable.pause_icon)
      this.playPauseButton.setOnClickListener { this.onPressedPause() }
      this.playPauseButton.contentDescription = this.getString(R.string.audiobook_accessibility_pause)
      this.configureSpineElementText(event.spineElement)
      this.playerPosition.isEnabled = true
      this.playerWaiting.text = ""
      this.onEventUpdateTimeRelatedUI(event.spineElement, event.offsetMilliseconds)
    })
  }

  private fun onEventUpdateTimeRelatedUI(
    spineElement: PlayerSpineElementType,
    offsetMilliseconds: Long) {

    this.playerPosition.max = spineElement.duration.standardSeconds.toInt()
    this.playerPosition.isEnabled = true

    this.playerPositionCurrentSpine = spineElement
    this.playerPositionCurrentOffset = offsetMilliseconds

    if (!this.playerPositionDragging) {
      this.playerPosition.progress =
        TimeUnit.MILLISECONDS.toSeconds(offsetMilliseconds).toInt()
    }

    this.playerTimeMaximum.text =
      PlayerTimeStrings.hourMinuteSecondTextFromDuration(spineElement.duration)
    this.playerTimeMaximum.contentDescription =
      this.playerTimeRemainingSpoken(offsetMilliseconds, spineElement.duration)

    this.playerTimeCurrent.text =
      PlayerTimeStrings.hourMinuteSecondTextFromMilliseconds(offsetMilliseconds)
    this.playerTimeCurrent.contentDescription =
      this.playerTimeCurrentSpoken(offsetMilliseconds)

    this.playerSpineElement.text = this.spineElementText(spineElement)
  }

  private fun playerTimeCurrentSpoken(offsetMilliseconds: Long): String {
    return this.getString(
      R.string.audiobook_accessibility_player_time_current,
      PlayerTimeStrings.hourMinuteSecondSpokenFromMilliseconds(this.timeStrings, offsetMilliseconds))
  }

  private fun playerTimeRemainingSpoken(
    offsetMilliseconds: Long,
    duration: Duration): String {

    val remaining =
      duration.minus(Duration.millis(offsetMilliseconds))

    return this.getString(
      R.string.audiobook_accessibility_player_time_remaining,
      PlayerTimeStrings.hourMinuteSecondSpokenFromDuration(this.timeStrings, remaining))
  }

  private fun spineElementText(spineElement: PlayerSpineElementType): String {
    return this.getString(
      R.string.audiobook_player_spine_element,
      spineElement.index + 1,
      spineElement.book.spine.size)
  }

  /**
   * Configure the chapter display (such as "Chapter 1 of 2") and update the accessibility content
   * description to give the same information.
   */

  private fun configureSpineElementText(element: PlayerSpineElementType) {
    this.playerSpineElement.text = this.spineElementText(element)
    this.playerSpineElement.contentDescription =
      this.getString(
        R.string.audiobook_accessibility_chapter_of,
        element.index + 1,
        this.book.spine.size)
  }

  /**
   * The player said that it has started buffering. Only display a message if it is still buffering
   * a few seconds from now.
   */

  private fun onPlayerBufferingStarted() {
    UIThread.runOnUIThread(Runnable {
      this.onPlayerBufferingStopTaskNow()
      this.playerBufferingStillOngoing = true
      this.playerBufferingTask =
        this.executor.schedule({ this.onPlayerBufferingCheckNow() }, 2L, TimeUnit.SECONDS)
    })
  }

  private fun onPlayerBufferingCheckNow() {
    UIThread.runOnUIThread(Runnable {
      if (this.playerBufferingStillOngoing) {
        val accessibleMessage = this.getString(R.string.audiobook_accessibility_player_buffering)
        this.playerWaiting.contentDescription = accessibleMessage
        this.playerWaiting.setText(R.string.audiobook_player_buffering)
        this.listener.onPlayerAccessibilityEvent(PlayerAccessibilityIsBuffering(accessibleMessage))
      }
    })
  }

  private fun onPlayerBufferingStopped() {
    UIThread.runOnUIThread(Runnable {
      this.onPlayerBufferingStopTaskNow()
      this.playerBufferingStillOngoing = false
    })
  }

  private fun onPlayerBufferingStopTaskNow() {
    val future = this.playerBufferingTask
    if (future != null) {
      future.cancel(true)
      this.playerBufferingTask = null
    }
  }
}
