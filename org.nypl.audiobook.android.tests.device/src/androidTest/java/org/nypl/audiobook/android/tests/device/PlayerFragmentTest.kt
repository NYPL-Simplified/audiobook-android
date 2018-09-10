package org.nypl.audiobook.android.tests.device

import android.os.Build
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.MediumTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.nypl.audiobook.android.api.PlayerPlaybackRate
import org.nypl.audiobook.android.views.PlayerPlaybackRateAdapter
import org.slf4j.LoggerFactory

@RunWith(AndroidJUnit4::class)
@MediumTest
class PlayerFragmentTest {

  private val log = LoggerFactory.getLogger(PlayerFragmentTest::class.java)

  @JvmField
  @Rule
  var activityRule: ActivityTestRule<MockPlayerActivity> =
    ActivityTestRule(MockPlayerActivity::class.java, false)

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
    testPlaybackRateDialogForRate(PlayerPlaybackRate.NORMAL_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksDouble() {
    testPlaybackRateDialogForRate(PlayerPlaybackRate.DOUBLE_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksThreeQuarters() {
    testPlaybackRateDialogForRate(PlayerPlaybackRate.THREE_QUARTERS_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksOneAndAHalf() {
    testPlaybackRateDialogForRate(PlayerPlaybackRate.ONE_AND_A_HALF_TIME)
  }

  /**
   * Test that using the playback rate dialog works.
   */

  @Test
  fun testPlaybackRateDialogWorksOneAndAQuarter() {
    testPlaybackRateDialogForRate(PlayerPlaybackRate.ONE_AND_A_QUARTER_TIME)
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

      Assert.assertEquals(1, calls.size)
      Assert.assertEquals("playbackRate " + rate, calls[0])
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
