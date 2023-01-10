package org.wordpress.android.reader.savedposts

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.reader.savedposts.ReaderSavedPostsAnalyticsTracker.ErrorType.Companion.EXPORTED_POSTS
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class ReaderSavedPostsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackStart() = analyticsTracker.track(Stat.READER_SAVED_POSTS_START)

    fun trackSuccess(numPosts: Int) = analyticsTracker.track(
        Stat.READER_SAVED_POSTS_SUCCESS, mapOf(EXPORTED_POSTS to numPosts)
    )

    fun trackFailed(errorType: ErrorType) =
        analyticsTracker.track(Stat.READER_SAVED_POSTS_FAILED, mapOf(ERROR_TYPE to errorType.value))

    sealed class ErrorType(open val value: String) {
        object QuerySavedPostsError : ErrorType("query_saved_posts_error")

        class GenericError(errorMessage: String?) : ErrorType(
            "generic_error: ${errorMessage?.take(EXCEPTION_MESSAGE_MAX_LENGTH) ?: ""}"
        )

        companion object {
            const val ERROR_TYPE = "error_type"
            const val EXPORTED_POSTS = "exported_posts"
            const val EXCEPTION_MESSAGE_MAX_LENGTH = 100
        }
    }
}
