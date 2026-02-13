package org.wordpress.android.ui.postsrs

import android.text.format.DateUtils
import uniffi.wp_api.PostStatus
import java.text.DateFormat
import java.util.Date

/**
 * Formats post dates from the wordpress-rs API for display in the
 * post list. Matches the date formatting behavior of the old
 * FluxC-based post list.
 */
object PostRsDateFormatter {
    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_HOUR = 3600L
    private const val HOURS_PER_DAY = 24L
    private const val DAYS_PER_YEAR = 365L
    private const val SECONDS_PER_YEAR =
        SECONDS_PER_HOUR * HOURS_PER_DAY * DAYS_PER_YEAR

    /**
     * Formats a GMT [Date] for display:
     * - Scheduled posts show an abbreviated absolute date
     * - Posts < 1 year ago show relative time ("2 hr. ago")
     * - Older posts show an abbreviated absolute date
     */
    fun format(dateGmt: Date, status: PostStatus?): String {
        val millis = dateGmt.time
        val now = System.currentTimeMillis()
        val secondsSince = (now - millis) / MILLIS_PER_SECOND
        val useRelative = status !is PostStatus.Future &&
            secondsSince < SECONDS_PER_YEAR

        return if (useRelative) {
            DateUtils.getRelativeTimeSpanString(
                millis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            ).toString()
        } else {
            formatAbbrDate(millis)
        }
    }

    private fun formatAbbrDate(millis: Long): String {
        val dateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM)
        return dateFormat.format(Date(millis))
    }
}
