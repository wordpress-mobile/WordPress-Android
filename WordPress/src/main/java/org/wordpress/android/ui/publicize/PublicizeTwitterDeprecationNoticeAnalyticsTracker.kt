package org.wordpress.android.ui.publicize

import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PublicizeTwitterDeprecationNoticeAnalyticsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    fun trackTwitterNoticeLinkTapped(source: Source) = analyticsTracker.track(
        AnalyticsTracker.Stat.TWITTER_NOTICE_LINK_TAPPED, mapOf(SOURCE_KEY to source.value)
    )

    sealed class Source(val value: String) {
        object List : Source("social_connection_list")
        object Detail : Source("social_connection_detail")
    }
}

private const val SOURCE_KEY = "source"
