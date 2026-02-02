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
    MOST_VIEWED_POSTS_AND_PAGES(R.string.stats_most_viewed_posts_and_pages, 2),
    MOST_VIEWED_REFERRERS(R.string.stats_most_viewed_referrers, 3),
    COUNTRIES(R.string.stats_countries_title, 4);

    companion object {
        /**
         * Returns the default list of visible cards in their default order.
         */
        fun defaultCards(): List<StatsCardType> = listOf(
            TODAYS_STATS,
            VIEWS_STATS,
            MOST_VIEWED_POSTS_AND_PAGES,
            MOST_VIEWED_REFERRERS,
            COUNTRIES
        )
    }
}
