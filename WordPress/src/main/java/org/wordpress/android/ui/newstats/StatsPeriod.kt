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
    data object Last6Months : StatsPeriod(R.string.stats_period_last_6_months)
    data object Last12Months : StatsPeriod(R.string.stats_period_last_12_months)
    data class Custom(val startDate: LocalDate, val endDate: LocalDate) :
        StatsPeriod(R.string.stats_period_custom)

    fun toTypeString(): String = when (this) {
        is Today -> PERIOD_TODAY
        is Last7Days -> PERIOD_LAST_7_DAYS
        is Last30Days -> PERIOD_LAST_30_DAYS
        is Last6Months -> PERIOD_LAST_6_MONTHS
        is Last12Months -> PERIOD_LAST_12_MONTHS
        is Custom -> PERIOD_CUSTOM
    }

    companion object {
        private const val PERIOD_TODAY = "today"
        private const val PERIOD_LAST_7_DAYS = "last_7_days"
        private const val PERIOD_LAST_30_DAYS = "last_30_days"
        private const val PERIOD_LAST_6_MONTHS = "last_6_months"
        private const val PERIOD_LAST_12_MONTHS = "last_12_months"
        private const val PERIOD_CUSTOM = "custom"

        /**
         * Returns all preset periods (excluding Custom which requires dates).
         */
        fun presets(): List<StatsPeriod> = listOf(
            Today,
            Last7Days,
            Last30Days,
            Last6Months,
            Last12Months
        )

        fun fromTypeString(
            type: String,
            customStartEpochDay: Long? = null,
            customEndEpochDay: Long? = null
        ): StatsPeriod = when (type) {
            PERIOD_TODAY -> Today
            PERIOD_LAST_7_DAYS -> Last7Days
            PERIOD_LAST_30_DAYS -> Last30Days
            PERIOD_LAST_6_MONTHS -> Last6Months
            PERIOD_LAST_12_MONTHS -> Last12Months
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
