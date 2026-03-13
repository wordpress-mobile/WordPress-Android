package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

enum class InsightsCardType(
    @StringRes val displayNameResId: Int
) {
    YEAR_IN_REVIEW(
        R.string.stats_insights_year_in_review
    ),
    ALL_TIME_STATS(
        R.string.stats_insights_all_time_stats_title
    ),
    MOST_POPULAR_DAY(
        R.string.stats_insights_most_popular_day
    ),
    MOST_POPULAR_TIME(
        R.string.stats_insights_most_popular_time
    ),
    TAGS_AND_CATEGORIES(
        R.string.stats_insights_tags_and_categories
    );

    companion object {
        fun defaultCards(): List<InsightsCardType> =
            listOf(
                YEAR_IN_REVIEW,
                ALL_TIME_STATS,
                MOST_POPULAR_DAY,
                MOST_POPULAR_TIME,
                TAGS_AND_CATEGORIES
            )
    }
}
