package org.wordpress.android.ui.postsrs

import android.text.format.DateUtils
import uniffi.wp_api.PostStatus
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

/**
 * Formats post dates from the wordpress-rs API for display in the
 * posts and pages lists. Mirrors the iOS app's date formatting
 * (see AbstractPostHelper / NSDate+Helpers): relative time only for
 * the last week, an absolute medium date beyond that.
 */
object PostRsDateFormatter {
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_HOUR = 3600L
    private const val HOURS_PER_DAY = 24L
    private const val DAYS_PER_WEEK = 7L
    private const val SECONDS_PER_WEEK =
        SECONDS_PER_HOUR * HOURS_PER_DAY * DAYS_PER_WEEK

    /**
     * Formats a GMT [Date] for display, matching the iOS posts/pages list:
     * - Scheduled posts show an absolute date with the time of day (e.g. "Dec 15, 2025, 9:00 AM")
     *   so the user can see when the post or page will publish.
     * - Other posts within the last week show relative time ("2 days ago", "Yesterday").
     * - Older posts show an abbreviated absolute date ("Dec 15, 2025").
     */
    fun format(dateGmt: Date, status: PostStatus?): String {
        val millis = dateGmt.time
        val now = System.currentTimeMillis()
        val isScheduled = status is PostStatus.Future
        val secondsSince = (now - millis) / MILLIS_PER_SECOND
        val useRelative = !isScheduled && abs(secondsSince) < SECONDS_PER_WEEK

        return when {
            useRelative -> DateUtils.getRelativeTimeSpanString(
                millis,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
            isScheduled -> formatAbbrDateTime(millis)
            else -> formatAbbrDate(millis)
        }
    }

    private fun formatAbbrDate(millis: Long): String {
        val dateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM)
        return dateFormat.format(Date(millis))
    }

    private fun formatAbbrDateTime(millis: Long): String {
        val dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return dateFormat.format(Date(millis))
    }
}
