package org.wordpress.android.ui.sitemonitor

import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class SiteMonitorUtilsTest {
    @Mock
    lateinit var userAgent: UserAgent

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var siteMonitorUtils: SiteMonitorUtils

    @Before
    fun setup() {
        siteMonitorUtils = SiteMonitorUtils(userAgent, analyticsTrackerWrapper)
    }

    @Test
    fun `when activity is launched, then event is tracked`() {
        siteMonitorUtils.trackActivityLaunched()

        verify(analyticsTrackerWrapper).track(AnalyticsTracker.Stat.SITE_MONITORING_SCREEN_SHOWN)
    }

    @Test
    fun `given url matches pattern, when sanitize is requested, then url is sanitized`() {
        val result = siteMonitorUtils.sanitizeSiteUrl("http://example.com")

        assertEquals("example.com", result)
    }

    @Test
    fun `given url is null, when sanitize is requested, then url is empty`() {
        val result = siteMonitorUtils.sanitizeSiteUrl(null)

        assertEquals("", result)
    }

    @Test
    fun `given url does not match pattern, when sanitize is requested, then url is not sanitized`() {
        val url = "gibberish"
        val result = siteMonitorUtils.sanitizeSiteUrl(url)

        assertEquals(url, result)
    }

    @Test
    fun `when metrics tab is launched, then event is tracked`() {
        siteMonitorUtils.trackTabLoaded(SiteMonitorType.METRICS)

        // Verify that the correct method was called on the analyticsTrackerWrapper
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_SHOWN,
            mapOf(
                SiteMonitorUtils.TAB_TRACK_KEY to SiteMonitorType.METRICS.analyticsDescription
            )
        )
    }

    @Test
    fun `when php logs tab is launched, then event is tracked`() {
        siteMonitorUtils.trackTabLoaded(SiteMonitorType.PHP_LOGS)

        // Verify that the correct method was called on the analyticsTrackerWrapper
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_SHOWN,
            mapOf(
                SiteMonitorUtils.TAB_TRACK_KEY to SiteMonitorType.PHP_LOGS.analyticsDescription
            )
        )
    }

    @Test
    fun `when web server logs tab is launched, then event is tracked`() {
        siteMonitorUtils.trackTabLoaded(SiteMonitorType.WEB_SERVER_LOGS)

        // Verify that the correct method was called on the analyticsTrackerWrapper
        verify(analyticsTrackerWrapper).track(
            AnalyticsTracker.Stat.SITE_MONITORING_TAB_SHOWN,
            mapOf(
                SiteMonitorUtils.TAB_TRACK_KEY to SiteMonitorType.WEB_SERVER_LOGS.analyticsDescription
            )
        )
    }
}
