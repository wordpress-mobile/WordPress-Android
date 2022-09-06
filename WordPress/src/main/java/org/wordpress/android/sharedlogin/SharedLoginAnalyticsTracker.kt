package org.wordpress.android.sharedlogin

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType.Companion.ERROR_TYPE
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class SharedLoginAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackLoginStart() = analyticsTracker.track(Stat.SHARED_LOGIN_START, emptyMap())

    fun trackLoginSuccess() = analyticsTracker.track(Stat.SHARED_LOGIN_SUCCESS, emptyMap())

    fun trackLoginFailed(errorType: ErrorType) =
            analyticsTracker.track(Stat.SHARED_LOGIN_FAILED, mapOf(ERROR_TYPE to errorType))

    sealed class ErrorType(val value: String) {
        object WPNotLoggedInError : ErrorType("wp_not_logged_in_error")

        object QueryTokenError : ErrorType("query_token_error")

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }
}
