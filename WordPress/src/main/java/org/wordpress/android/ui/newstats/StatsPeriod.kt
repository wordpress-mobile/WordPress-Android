package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R
import java.time.LocalDate

/**
 * Represents the different time periods available for stats viewing.
 */
sealed class StatsPeriod(@StringRes val labelResId: Int) {
    data object Today : StatsPeriod(R.string.stats_period_today)
    data object Last7Days : StatsPeriod(R.string.stats_period_last_7_days)
    data object Last30Days : StatsPeriod(R.string.stats_period_last_30_days)
    data object Last12Months : StatsPeriod(R.string.stats_period_last_12_months)
    data object ThisWeek : StatsPeriod(R.string.stats_period_this_week)
    data object ThisMonth : StatsPeriod(R.string.stats_period_this_month)
    data object ThisYear : StatsPeriod(R.string.stats_period_this_year)
    data class Custom(val startDate: LocalDate, val endDate: LocalDate) :
        StatsPeriod(R.string.stats_period_custom)

    fun toTypeString(): String = when (this) {
        is Today -> PERIOD_TODAY
        is Last7Days -> PERIOD_LAST_7_DAYS
        is Last30Days -> PERIOD_LAST_30_DAYS
        is Last12Months -> PERIOD_LAST_12_MONTHS
        is ThisWeek -> PERIOD_THIS_WEEK
        is ThisMonth -> PERIOD_THIS_MONTH
        is ThisYear -> PERIOD_THIS_YEAR
        is Custom -> PERIOD_CUSTOM
    }

    companion object {
        private const val PERIOD_TODAY = "today"
        private const val PERIOD_LAST_7_DAYS = "last_7_days"
        private const val PERIOD_LAST_30_DAYS = "last_30_days"
        private const val PERIOD_LAST_12_MONTHS = "last_12_months"
        private const val PERIOD_THIS_WEEK = "this_week"
        private const val PERIOD_THIS_MONTH = "this_month"
        private const val PERIOD_THIS_YEAR = "this_year"
        private const val PERIOD_CUSTOM = "custom"

        /**
         * The rolling "last N" presets, shown as the first group of the period selector.
         */
        fun rollingPresets(): List<StatsPeriod> = listOf(
            Last7Days,
            Last30Days,
            Last12Months
        )

        /**
         * The calendar-aligned presets (Today plus this week/month/year to date), shown as the
         * second group of the period selector.
         */
        fun calendarPresets(): List<StatsPeriod> = listOf(
            Today,
            ThisWeek,
            ThisMonth,
            ThisYear
        )

        /**
         * Returns all preset periods (excluding Custom which requires dates).
         */
        fun presets(): List<StatsPeriod> = rollingPresets() + calendarPresets()

        fun fromTypeString(
            type: String,
            customStartEpochDay: Long? = null,
            customEndEpochDay: Long? = null
        ): StatsPeriod = when (type) {
            PERIOD_TODAY -> Today
            PERIOD_LAST_7_DAYS -> Last7Days
            PERIOD_LAST_30_DAYS -> Last30Days
            PERIOD_LAST_12_MONTHS -> Last12Months
            PERIOD_THIS_WEEK -> ThisWeek
            PERIOD_THIS_MONTH -> ThisMonth
            PERIOD_THIS_YEAR -> ThisYear
            PERIOD_CUSTOM -> {
                if (customStartEpochDay != null &&
                    customEndEpochDay != null
                ) {
                    Custom(
                        LocalDate.ofEpochDay(
                            customStartEpochDay
                        ),
                        LocalDate.ofEpochDay(
                            customEndEpochDay
                        )
                    )
                } else {
                    Last7Days
                }
            }
            else -> Last7Days
        }
    }
}
