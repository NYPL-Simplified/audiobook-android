package org.librarysimplified.audiobook.views

import android.content.res.Resources
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder

object PlayerTimeStrings {

  /**
   * Spoken translations for words.
   */

  data class SpokenTranslations(

    /**
     * The word for "hours" in the current language.
     */

    val hoursText: String,

    /**
     * The word for "hour" in the current language.
     */

    val hourText: String,

    /**
     * The word for "minutes" in the current language.
     */

    val minutesText: String,

    /**
     * The word for "minute" in the current language.
     */

    val minuteText: String,

    /**
     * The word for "seconds" in the current language.
     */

    val secondsText: String,

    /**
     * The word for "second" in the current language.
     */

    val secondText: String) {

    fun minutes(minutes: Long): String =
      if (minutes > 1) this.minutesText else this.minuteText

    fun hours(hours: Long): String =
      if (hours > 1) this.hoursText else this.hourText

    fun seconds(seconds: Long): String =
      if (seconds > 1) this.secondsText else this.secondText

    companion object {

      fun createFromResources(resources: Resources): SpokenTranslations {
        return SpokenTranslations(
          hoursText = resources.getString(R.string.audiobook_accessibility_hours),
          hourText = resources.getString(R.string.audiobook_accessibility_hour),
          minutesText = resources.getString(R.string.audiobook_accessibility_minutes),
          minuteText = resources.getString(R.string.audiobook_accessibility_minute),
          secondsText = resources.getString(R.string.audiobook_accessibility_seconds),
          secondText = resources.getString(R.string.audiobook_accessibility_second))
      }
    }
  }

  private val hourMinuteSecondFormatter: PeriodFormatter =
    PeriodFormatterBuilder()
      .printZeroAlways()
      .minimumPrintedDigits(2)
      .appendHours()
      .appendLiteral(":")
      .appendMinutes()
      .appendLiteral(":")
      .appendSeconds()
      .toFormatter()

  private val minuteSecondFormatter: PeriodFormatter =
    PeriodFormatterBuilder()
      .printZeroAlways()
      .minimumPrintedDigits(2)
      .appendMinutes()
      .appendLiteral(":")
      .appendSeconds()
      .toFormatter()

  fun hourMinuteSecondTextFromMilliseconds(milliseconds: Long): String {
    return hourMinuteSecondFormatter.print(Duration.millis(milliseconds).toPeriod())
  }

  fun hourMinuteSecondSpokenFromMilliseconds(
    translations: SpokenTranslations,
    milliseconds: Long): String {
    return hourMinuteSecondSpokenFromDuration(translations, Duration.millis(milliseconds))
  }

  fun hourMinuteSecondTextFromDuration(duration: Duration): String {
    return hourMinuteSecondFormatter.print(duration.toPeriod())
  }

  fun hourMinuteSecondSpokenFromDuration(
    translations: SpokenTranslations,
    duration: Duration): String {

    val builder = StringBuilder(64)
    var period = duration.toPeriod()

    val hours = period.toStandardHours()
    if (hours.hours > 0) {
      builder.append(hours.hours)
      builder.append(' ')
      builder.append(translations.hours(hours.hours.toLong()))
      builder.append(' ')
      period = period.minus(hours)
    }

    val minutes = period.toStandardMinutes()
    if (minutes.minutes > 0) {
      builder.append(minutes.minutes)
      builder.append(' ')
      builder.append(translations.minutes(minutes.minutes.toLong()))
      builder.append(' ')
      period = period.minus(Period.minutes(minutes.minutes))
    }

    val seconds = period.toStandardSeconds()
    if (seconds.seconds > 0) {
      builder.append(seconds.seconds)
      builder.append(' ')
      builder.append(translations.seconds(seconds.seconds.toLong()))
    }

    return builder.toString().trim()
  }

  fun minuteSecondTextFromDuration(duration: Duration): String {
    return minuteSecondFormatter.print(duration.toPeriod())
  }

  fun minuteSecondSpokenFromDuration(
    translations: SpokenTranslations,
    duration: Duration): String {

    val builder = StringBuilder(64)
    var period = duration.toPeriod()

    val minutes = period.toStandardMinutes()
    if (minutes.minutes > 0) {
      builder.append(minutes.minutes)
      builder.append(' ')
      builder.append(translations.minutes(minutes.minutes.toLong()))
      builder.append(' ')
      period = period.minus(Period.minutes(minutes.minutes))
    }

    val seconds = period.toStandardSeconds()
    if (seconds.seconds > 0) {
      builder.append(seconds.seconds)
      builder.append(' ')
      builder.append(translations.seconds(seconds.seconds.toLong()))
    }

    return builder.toString().trim()
  }
}
