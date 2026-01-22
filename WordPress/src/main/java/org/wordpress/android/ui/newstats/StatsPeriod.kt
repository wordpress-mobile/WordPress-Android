package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class StatsPeriod(@StringRes val labelResId: Int) {
    TODAY(R.string.stats_period_today),
    LAST_7_DAYS(R.string.stats_period_last_7_days),
    LAST_30_DAYS(R.string.stats_period_last_30_days),
    LAST_6_MONTHS(R.string.stats_period_last_6_months),
    LAST_12_MONTHS(R.string.stats_period_last_12_months),
    CUSTOM(R.string.stats_period_custom)
}
