package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class InsightsCardType(
    @StringRes val displayNameResId: Int
) {
    YEAR_IN_REVIEW(R.string.stats_insights_year_in_review);

    companion object {
        fun defaultCards(): List<InsightsCardType> = listOf(
            YEAR_IN_REVIEW
        )
    }
}
