package org.wordpress.android.ui.stats.refresh.utils

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.ONE_DAY_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.ONE_HOUR_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.ONE_MINUTE_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.ONE_MONTH_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.ONE_YEAR_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.SECONDS_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.X_DAYS_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.X_HOURS_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.X_MINUTES_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.X_MONTHS_AGO
import org.wordpress.android.ui.stats.refresh.utils.StatsSinceLabelFormatter.Interval.X_YEARS_AGO
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.FOREVER
import java.time.temporal.ChronoUnit.HOURS
import java.time.temporal.ChronoUnit.MINUTES
import java.time.temporal.ChronoUnit.MONTHS
import java.time.temporal.ChronoUnit.YEARS
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * ┌──────────────────────────────┬───────────────────────────┐
 * │ Interval                     │ Label                     │
 * ├──────────────────────────────┼───────────────────────────┤
 * │ 0 to 44 seconds              │ seconds ago               │
 * │ 45 to 89 seconds             │ a minute ago              │
 * │ 90 seconds to 44 minutes     │ 2 minutes ... 45 minutes  │
 * │ 45 to 90 minutes             │ an hour ago               │
 * │ 90 minutes to 21 hours       │ 2 hours ... 22 hours      │
 * │ 22 to 35 hours               │ a day                     │
 * │ 36 hours to 25 days          │ 2 days ... 25 days        │
 * │ 26 to 44 days                │ a month                   │
 * │ 45 days to 319 days          │ 2 months ... 11 months    │
 * │ 320 to 547 days (1.5 years)  │ a year                    │
 * │ 548 days+                    │ 2 years ... 20 years      │
 * └──────────────────────────────┴───────────────────────────┘
 *
 * Based on http://momentjs.com/docs/#/displaying/fromnow/
 */
class StatsSinceLabelFormatter @Inject constructor(
    val resourceProvider: ResourceProvider
) {
    fun getSinceLabelLowerCase(date: Date) = getSinceLabel(date).lowercase(Locale.getDefault())

    fun getSinceLabel(date: Date) = getSinceLabel(date.toInstant().atZone(ZoneId.systemDefault()))

    fun getPublishedSinceLabel(date: Date) = getPublishedSinceLabel(date.toInstant().atZone(ZoneId.systemDefault()))

    fun getPublishedSinceLabel(
        date: ZonedDateTime,
        now: ZonedDateTime = ZonedDateTime.now()
    ) = when (Interval.between(date, now)) {
        SECONDS_AGO -> getLabelString(R.string.stats_published_seconds_ago)
        ONE_MINUTE_AGO -> getLabelString(R.string.stats_published_a_minute_ago)
        X_MINUTES_AGO -> getLabelString(R.string.stats_published_minutes_ago, MINUTES.between(date, now))
        ONE_HOUR_AGO -> getLabelString(R.string.stats_published_an_hour_ago)
        X_HOURS_AGO -> getLabelString(R.string.stats_published_hours_ago, HOURS.between(date, now))
        ONE_DAY_AGO -> getLabelString(R.string.stats_published_a_day_ago)
        X_DAYS_AGO -> getLabelString(R.string.stats_published_days_ago, DAYS.between(date, now))
        ONE_MONTH_AGO -> getLabelString(R.string.stats_published_a_month_ago)
        X_MONTHS_AGO -> getLabelString(R.string.stats_published_months_ago, MONTHS.between(date, now))
        ONE_YEAR_AGO -> getLabelString(R.string.stats_published_a_year_ago)
        X_YEARS_AGO -> getLabelString(R.string.stats_published_years_ago, YEARS.between(date, now))
    }

    fun getSinceLabel(
        date: ZonedDateTime,
        now: ZonedDateTime = ZonedDateTime.now()
    ) = when (Interval.between(date, now)) {
        SECONDS_AGO -> getLabelString(R.string.stats_followers_seconds_ago)
        ONE_MINUTE_AGO -> getLabelString(R.string.stats_followers_a_minute_ago)
        X_MINUTES_AGO -> getLabelString(R.string.stats_followers_minutes, MINUTES.between(date, now))
        ONE_HOUR_AGO -> getLabelString(R.string.stats_followers_an_hour_ago)
        X_HOURS_AGO -> getLabelString(R.string.stats_followers_hours, HOURS.between(date, now))
        ONE_DAY_AGO -> getLabelString(R.string.stats_followers_a_day)
        X_DAYS_AGO -> getLabelString(R.string.stats_followers_days, DAYS.between(date, now))
        ONE_MONTH_AGO -> getLabelString(R.string.stats_followers_a_month)
        X_MONTHS_AGO -> getLabelString(R.string.stats_followers_months, MONTHS.between(date, now))
        ONE_YEAR_AGO -> getLabelString(R.string.stats_followers_a_year)
        X_YEARS_AGO -> getLabelString(R.string.stats_followers_years, YEARS.between(date, now))
    }

    private fun getLabelString(@StringRes labelId: Int, labelTime: Long? = null) = labelTime
            ?.let { resourceProvider.getString(labelId, labelTime.roundUp()) }
            ?: resourceProvider.getString(labelId)

    private fun Long.roundUp(increment: Double = 0.5) = plus(increment).roundToLong()

    private enum class Interval(val threshold: Duration) {
        SECONDS_AGO(Duration.ofSeconds(45)),
        ONE_MINUTE_AGO(Duration.ofSeconds(90)),
        X_MINUTES_AGO(Duration.ofMinutes(45)),
        ONE_HOUR_AGO(Duration.ofMinutes(90)),
        X_HOURS_AGO(Duration.ofHours(22)),
        ONE_DAY_AGO(Duration.ofHours(36)),
        X_DAYS_AGO(Duration.ofDays(25)),
        ONE_MONTH_AGO(Duration.ofDays(45)),
        X_MONTHS_AGO(Duration.ofDays(320)),
        ONE_YEAR_AGO(Duration.ofDays(548)),
        X_YEARS_AGO(FOREVER.duration);

        companion object {
            fun between(
                startDate: ZonedDateTime,
                endDate: ZonedDateTime
            ) = values().first { Duration.between(startDate, endDate) < it.threshold }
        }
    }
}
