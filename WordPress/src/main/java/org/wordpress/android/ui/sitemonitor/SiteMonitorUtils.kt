package org.wordpress.android.ui.sitemonitor

import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class SiteMonitorUtils @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun getUserAgent() = WordPress.getUserAgent()

    fun getAuthenticationPostData(authenticationUrl: String,
                                  urlToLoad: String,
                                  username: String,
                                  password: String,
                                  token: String): String =
        WPWebViewActivity.getAuthenticationPostData(authenticationUrl, urlToLoad, username, password, token)


    fun trackActivityLaunched() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }

    fun sanitizeSiteUrl(url: String?) = url?.replace(Regex(HTTP_PATTERN), "") ?: ""

    companion object {
        const val HTTP_PATTERN = "(https?://)"
    }
}
