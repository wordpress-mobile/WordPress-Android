package org.wordpress.android.ui.newstats.subscribers

import androidx.annotation.StringRes
import org.wordpress.android.R

/**
 * Defines the available card types for the Subscribers tab.
 * Each card has a unique identifier and display name resource.
 */
enum class SubscribersCardType(
    @StringRes val displayNameResId: Int
) {
    ALL_TIME_SUBSCRIBERS(R.string.stats_subscribers_all_time),
    SUBSCRIBERS_GRAPH(R.string.stats_subscribers_graph),
    SUBSCRIBERS_LIST(R.string.stats_subscribers_list),
    EMAILS(R.string.stats_subscribers_emails);

    companion object {
        /**
         * Returns the default list of visible cards
         * in their default order.
         */
        fun defaultCards(): List<SubscribersCardType> =
            listOf(
                ALL_TIME_SUBSCRIBERS,
                SUBSCRIBERS_GRAPH,
                SUBSCRIBERS_LIST,
                EMAILS
            )
    }
}
