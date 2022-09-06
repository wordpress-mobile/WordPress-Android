package org.wordpress.android.sharedlogin

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class SharedLoginAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackLoginStart() = analyticsTracker.track(Stat.SHARED_LOGIN_START, emptyMap())

    fun trackLoginSuccess() = analyticsTracker.track(Stat.SHARED_LOGIN_SUCCESS, emptyMap())

    fun trackLoginFailed() = analyticsTracker.track(Stat.SHARED_LOGIN_FAILED, emptyMap())
}
