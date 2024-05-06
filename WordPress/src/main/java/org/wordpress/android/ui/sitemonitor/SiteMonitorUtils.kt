package org.wordpress.android.ui.sitemonitor

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class SiteMonitorUtils @Inject constructor(
    private val userAgent: UserAgent,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun getUserAgent() = userAgent.toString()

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

    fun trackTabLoaded(siteMonitorType: SiteMonitorType) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_SHOWN,
            mapOf(
                TAB_TRACK_KEY to siteMonitorType.analyticsDescription
            ))
    }

    fun trackTabLoadingError(siteMonitorType: SiteMonitorType) {
        analyticsTrackerWrapper.track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_LOADING_ERROR,
            mapOf(
                TAB_TRACK_KEY to siteMonitorType.analyticsDescription
            ))
    }

    companion object {
        const val HTTP_PATTERN = "(https?://)"
        const val PHP_LOGS_PATTERN = "/php"
        const val WEB_SERVER_LOGS_PATTERN = "/web"
        const val TAB_TRACK_KEY = "tab"
    }
}
