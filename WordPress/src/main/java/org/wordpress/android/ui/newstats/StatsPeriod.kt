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

    companion object {
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
    }
}
