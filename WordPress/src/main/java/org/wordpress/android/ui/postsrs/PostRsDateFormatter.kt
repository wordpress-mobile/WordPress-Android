package org.wordpress.android.ui.postsrs

import android.text.format.DateUtils
import uniffi.wp_api.PostStatus
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats post date strings from the wordpress-rs API for display
 * in the post list. Matches the date formatting behavior of the
 * old FluxC-based post list.
 */
object PostRsDateFormatter {
    private val postDateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }

    private const val SECONDS_PER_DAY = 60L * 60 * 24
    private const val DAYS_PER_YEAR = 365L

    /**
     * Formats a post date string:
     * - Scheduled posts show an abbreviated absolute date
     * - Posts < 1 year ago show relative time ("2 hr. ago")
     * - Older posts show an abbreviated absolute date
     */
    @Suppress("TooGenericExceptionCaught")
    fun format(dateString: String, status: PostStatus?): String {
        if (dateString.isBlank()) return dateString
        val millis = try {
            postDateFormat.get()?.parse(dateString)?.time
        } catch (_: Exception) {
            null
        } ?: return dateString

        if (status is PostStatus.Future) {
            return formatAbbrDate(millis)
        }

        val now = System.currentTimeMillis()
        val secondsSince = (now - millis) / 1000
        if (secondsSince < SECONDS_PER_DAY * DAYS_PER_YEAR) {
            return DateUtils.getRelativeTimeSpanString(
                millis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL
            ).toString()
        }
        return formatAbbrDate(millis)
    }

    private fun formatAbbrDate(millis: Long): String {
        val dateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM)
        return dateFormat.format(Date(millis))
    }
}
