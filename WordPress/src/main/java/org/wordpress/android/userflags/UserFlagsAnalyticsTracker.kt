package org.wordpress.android.userflags

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class UserFlagsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackStart() = analyticsTracker.track(Stat.USER_FLAGS_START)

    fun trackSuccess() = analyticsTracker.track(Stat.USER_FLAGS_SUCCESS)

    fun trackFailed(errorType: ErrorType) =
            analyticsTracker.track(Stat.USER_FLAGS_FAILED, mapOf(ERROR_TYPE to errorType.value))

    sealed class ErrorType(val value: String) {
        object NoUserFlagsFoundError : ErrorType("no_user_flags_found_error")

        object QueryUserFlagsError : ErrorType("query_user_flags_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}
