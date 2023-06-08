package org.wordpress.android.ui.publicize

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.publicize.PublicizeTwitterDeprecationNoticeAnalyticsTracker.Source
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class PublicizeTwitterDeprecationNoticeAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = PublicizeTwitterDeprecationNoticeAnalyticsTracker(
        analyticsTracker = analyticsTrackerWrapper,
    )

    @Test
    fun `Should track Twitter notice link tapped when trackTwitterNoticeLinkTapped is called with Source List`() {
        classToTest.trackTwitterNoticeLinkTapped(Source.List)
        verify(analyticsTrackerWrapper)
            .track(
                AnalyticsTracker.Stat.TWITTER_NOTICE_LINK_TAPPED,
                mapOf("source" to "social_connection_list")
            )
    }

    @Test
    fun `Should track Twitter notice link tapped when trackTwitterNoticeLinkTapped is called with Source Detail`() {
        classToTest.trackTwitterNoticeLinkTapped(Source.Detail)
        verify(analyticsTrackerWrapper)
            .track(
                AnalyticsTracker.Stat.TWITTER_NOTICE_LINK_TAPPED,
                mapOf("source" to "social_connection_detail")
            )
    }
}
