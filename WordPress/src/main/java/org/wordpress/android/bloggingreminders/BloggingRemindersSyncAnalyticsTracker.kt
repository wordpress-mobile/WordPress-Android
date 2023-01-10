package org.wordpress.android.bloggingreminders

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.bloggingreminders.BloggingRemindersSyncAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BloggingRemindersSyncAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackStart() = analyticsTracker.track(Stat.BLOGGING_REMINDERS_SYNC_START)

    fun trackSuccess(remindersSyncedCount: Int) = analyticsTracker.track(
        Stat.BLOGGING_REMINDERS_SYNC_SUCCESS, mapOf(REMINDERS_SYNCED_COUNT to remindersSyncedCount)
    )

    fun trackFailed(errorType: ErrorType) =
        analyticsTracker.track(Stat.BLOGGING_REMINDERS_SYNC_FAILED, mapOf(ERROR_TYPE to errorType.value))

    sealed class ErrorType(val value: String) {
        object QueryBloggingRemindersError : ErrorType("query_blogging_reminders_error")

        object UpdateBloggingRemindersError : ErrorType("update_blogging_reminders_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}

const val REMINDERS_SYNCED_COUNT = "reminders_synced_count"
