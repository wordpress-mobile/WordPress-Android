package org.wordpress.android.ui.newstats

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * Defines the available card types for the new stats screen.
 * Each card has a unique identifier and display name resource.
 */
enum class StatsCardType(
    @StringRes val displayNameResId: Int
) {
    TODAYS_STATS(R.string.stats_insights_today),
    VIEWS_STATS(R.string.stats_views),
    MOST_VIEWED_POSTS_AND_PAGES(R.string.stats_most_viewed_posts_and_pages),
    MOST_VIEWED_REFERRERS(R.string.stats_most_viewed_referrers),
    LOCATIONS(R.string.stats_countries_location_header),
    AUTHORS(R.string.stats_authors_title);

    companion object {
        /**
         * Returns the default list of visible cards in their default order.
         */
        fun defaultCards(): List<StatsCardType> = listOf(
            TODAYS_STATS,
            VIEWS_STATS,
            MOST_VIEWED_POSTS_AND_PAGES,
            MOST_VIEWED_REFERRERS,
            LOCATIONS,
            AUTHORS
        )
    }
}
