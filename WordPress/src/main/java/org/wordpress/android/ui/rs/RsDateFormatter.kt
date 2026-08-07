package org.wordpress.android.ui.rs

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

/**
 * Formats dates from the wordpress-rs API for display in the posts, pages and comments
 * lists, so all three read the same way. Mirrors the iOS app's date formatting
 * (see AbstractPostHelper / NSDate+Helpers): relative time only for the last week,
 * an absolute medium date beyond that.
 */
object RsDateFormatter {
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3600L
    private const val HOURS_PER_DAY = 24L
    private const val DAYS_PER_WEEK = 7L
    private const val SECONDS_PER_WEEK =
        SECONDS_PER_HOUR * HOURS_PER_DAY * DAYS_PER_WEEK

    /**
     * Formats a GMT [Date] for display, matching the iOS posts/pages list:
     * - Scheduled posts ([isScheduled]) show an absolute date with the time of day
     *   (e.g. "Dec 15, 2025, 9:00 AM") so the user can see when it will publish.
     * - Anything less than a minute old shows [nowLabel], since the relative formatter would
     *   otherwise render a just-created post or a fresh comment as "0 minutes ago".
     * - Anything else within the last week shows relative time ("2 days ago", "Yesterday").
     * - Older dates show an abbreviated absolute date ("Dec 15, 2025").
     */
    fun format(dateGmt: Date, nowLabel: String, isScheduled: Boolean = false): String {
        val millis = dateGmt.time
        val now = System.currentTimeMillis()
        // The server's date can sit slightly ahead of the device clock, so treat a sub-minute
        // gap in either direction as "now" rather than letting it read "In 0 minutes".
        val secondsSince = abs((now - millis) / MILLIS_PER_SECOND)

        return when {
            isScheduled -> formatAbbrDateTime(millis)
            secondsSince < SECONDS_PER_MINUTE -> nowLabel
            secondsSince < SECONDS_PER_WEEK ->
                DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.MINUTE_IN_MILLIS).toString()
            else -> formatAbbrDate(millis)
        }
    }

    private fun formatAbbrDate(millis: Long) =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

    private fun formatAbbrDateTime(millis: Long) =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}
