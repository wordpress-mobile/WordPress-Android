package org.wordpress.android.ui.sitemonitor

import android.util.Log
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
        Log.i(javaClass.simpleName, "track Site Monitor screen shown")
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }

    fun sanitizeSiteUrl(url: String?) = url?.replace(Regex(HTTP_PATTERN), "") ?: ""

    fun urlToType(url: String): SiteMonitorType {
        return when {
            url.contains(PHP_LOGS_PATTERN) -> SiteMonitorType.PHP_LOGS
            url.contains(WEB_SERVER_LOGS_PATTERN) -> SiteMonitorType.WEB_SERVER_LOGS
            else -> SiteMonitorType.METRICS
        }
    }

    fun trackTabLoaded(siteMonitorType: SiteMonitorType) {
        Log.i(javaClass.simpleName, "track TabLoaded with $siteMonitorType")
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_SHOWN,
            mapOf(
                TAB_TRACK_KEY to siteMonitorType
            ))
    }

    companion object {
        const val HTTP_PATTERN = "(https?://)"
        const val PHP_LOGS_PATTERN = "/php"
        const val WEB_SERVER_LOGS_PATTERN = "/web"
        const val TAB_TRACK_KEY = "tab"
    }
}
