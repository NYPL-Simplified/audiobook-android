package org.librarysimplified.audiobook.tests.device

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librarysimplified.audiobook.api.PlayerPlaybackRate
import org.librarysimplified.audiobook.views.PlayerPlaybackRateAdapter
import org.nypl.audiobook.android.tests.device.R
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerFragmentTest {

  private val log = LoggerFactory.getLogger(PlayerFragmentTest::class.java)

  @JvmField
  @Rule
  var activityRule: ActivityTestRule<MockPlayerActivity> =
    ActivityTestRule(MockPlayerActivity::class.java, false)

  private lateinit var wakeLock: PowerManager.WakeLock

  /**
   * Acquire a wake lock so that the device doesn't go to sleep during testing.
   */

  @Before
  fun setup() {
    this.log.debug("waking up device")

    val act = this.activityRule.activity
    val keyguard = act.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    keyguard.newKeyguardLock(this.javaClass.simpleName).disableKeyguard()

    this.log.debug("acquiring wake lock")
    val power = act.getSystemService(Context.POWER_SERVICE) as PowerManager
    this.wakeLock = power.newWakeLock(
      PowerManager.FULL_WAKE_LOCK
        or PowerManager.ACQUIRE_CAUSES_WAKEUP
        or PowerManager.ON_AFTER_RELEASE,
      this.javaClass.simpleName)
    this.wakeLock.acquire()
  }

  @After
  fun tearDown() {
    this.log.debug("releasing wake lock")
    this.wakeLock.release()
  }

  /**
   * Test that the play button does actually tell the player to play.
   */

  @Test
  fun testPlayButtonWorks() {
    val activity = this.activityRule.activity

    val calls = this.logCalls(activity)

    Espresso.onView(ViewMatchers.withId(R.id.player_play_button))
      .perform(ViewActions.click())

    Assert.assertEquals(1, calls.size)
    Assert.assertEquals("play", calls[0])
  }

  /**
   * Test that the forward button does actually tell the player to skip forward.
   */

  @Test
  fun testForwardButtonWorks() {
    val activity = this.activityRule.activity

    val calls = this.logCalls(activity)

    Espresso.onView(ViewMatchers.withId(R.id.player_jump_forwards))
      .perform(ViewActions.click())

    Assert.assertEquals(1, calls.size)
    Assert.assertEquals("skipForward", calls[0])
  }

  /**
   * Test that the backward button does actually tell the player to skip backward.
   */

  @Test
  fun testBackwardButtonWorks() {
    val activity = this.activityRule.activity

    val calls = this.logCalls(activity)

    Espresso.onView(ViewMatchers.withId(R.id.player_jump_backwards))
      .perform(ViewActions.click())

    Assert.assertEquals(1, calls.size)
    Assert.assertEquals("skipBack", calls[0])
  }

  /**
   * Test that opening the sleep timer dialog works.
   */

  @Test
  fun testSleepDialogWorks() {
    Espresso.onView(ViewMatchers.withId(R.id.player_menu_sleep))
      .perform(ViewActions.click())

    Espresso.onView(Matchers.allOf(
      ViewMatchers.withText(R.string.audiobook_player_menu_sleep_title),
      ViewMatchers.isDisplayed()))
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksNormal() {
    this.testPlaybackRateDialogForRate(PlayerPlaybackRate.NORMAL_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksDouble() {
    this.testPlaybackRateDialogForRate(PlayerPlaybackRate.DOUBLE_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksThreeQuarters() {
    this.testPlaybackRateDialogForRate(PlayerPlaybackRate.THREE_QUARTERS_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksOneAndAHalf() {
    this.testPlaybackRateDialogForRate(PlayerPlaybackRate.ONE_AND_A_HALF_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksOneAndAQuarter() {
    this.testPlaybackRateDialogForRate(PlayerPlaybackRate.ONE_AND_A_QUARTER_TIME)
  }

  private fun testPlaybackRateDialogForRate(rate: PlayerPlaybackRate) {
    if (Build.VERSION.SDK_INT >= 23) {
      val activity = this.activityRule.activity
      val calls = this.logCalls(activity)

      Espresso.onView(ViewMatchers.withId(R.id.player_menu_playback_rate))
        .perform(ViewActions.click())

      Espresso.onView(Matchers.allOf(
        ViewMatchers.withText(R.string.audiobook_player_menu_playback_rate_title),
        ViewMatchers.isDisplayed()))

      Espresso.onView(ViewMatchers.withText(
        PlayerPlaybackRateAdapter.textOfRate(rate)))
        .perform(ViewActions.click())

      if (rate != PlayerPlaybackRate.NORMAL_TIME) {
        Assert.assertEquals(1, calls.size)
        Assert.assertEquals("playbackRate " + rate, calls[0])
      } else {
        Assert.assertEquals(0, calls.size)
      }
    }
  }

  private fun logCalls(activity: MockPlayerActivity): ArrayList<String> {
    val calls = ArrayList<String>()
    activity.player.calls.subscribe(
      { event -> this.log.debug("{}", event); calls.add(event) },
      { error -> this.log.debug("{}", error); },
      { })
    return calls
  }

}
