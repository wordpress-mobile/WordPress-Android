package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * Defines the available card types for the new stats screen.
 * Each card has a unique identifier, display name resource, and default order.
 */
enum class StatsCardType(
    @StringRes val displayNameResId: Int,
    val defaultOrder: Int
) {
    TODAYS_STATS(R.string.stats_insights_today, 0),
    VIEWS_STATS(R.string.stats_views, 1),
    MOST_VIEWED(R.string.stats_most_viewed_title, 2),
    COUNTRIES(R.string.stats_countries_title, 3);

    companion object {
        /**
         * Returns the default list of visible cards in their default order.
         * Note: MOST_VIEWED appears twice by default (one for each data source).
         */
        fun defaultCards(): List<StatsCardType> = listOf(
            TODAYS_STATS,
            VIEWS_STATS,
            MOST_VIEWED,
            MOST_VIEWED,
            COUNTRIES
        )
    }
}
