package org.wordpress.android.ui.rs.contentlist

/**
 * The scroll-driven timings and thresholds the rs content lists share.
 *
 * Kept together and in one place because they are tuned against each other: the debounce has to be
 * short enough that a stopped scroll fetches promptly and long enough that a fling does not fetch
 * for every row it passes, and the load-more threshold has to fire before the debounce does.
 */
object ContentListDefaults {
    /** How long the visible-row set must settle before metrics are fetched for it. */
    const val VISIBLE_ROWS_DEBOUNCE_MS = 300L

    /** How close to the end of the list the user has to scroll before the next page is asked for. */
    const val LOAD_MORE_THRESHOLD = 5

    /** Roughly a screenful, so the skeleton fills the list rather than leaving a gap below it. */
    const val SHIMMER_ITEM_COUNT = 8

    /**
     * How long a reveal waits for the refresh carrying the new post or page to land before giving
     * up. Long enough for a slow site, short enough that it cannot scroll the list under someone
     * who has moved on.
     */
    const val REVEAL_TIMEOUT_MS = 15_000L
}
