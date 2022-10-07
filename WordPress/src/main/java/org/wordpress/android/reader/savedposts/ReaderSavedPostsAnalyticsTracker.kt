package org.wordpress.android.reader.savedposts

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class ReaderSavedPostsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackStart() = analyticsTracker.track(Stat.READER_SAVED_POSTS_START)

    fun trackSuccess() = analyticsTracker.track(Stat.READER_SAVED_POSTS_SUCCESS)

    fun trackFailed(errorType: ErrorType) =
            analyticsTracker.track(Stat.READER_SAVED_POSTS_FAILED, mapOf(ERROR_TYPE to errorType.value))

    sealed class ErrorType(val value: String) {
        object NoUserSavedPostsError : ErrorType("no_saved_posts_found_error")

        object QuerySavedPostsError : ErrorType("query_saved_posts_error")

        object UpdateSavedPostsError : ErrorType("update_saved_posts_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}
