package org.wordpress.android.ui.stats.refresh.utils

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class StatsAnalyticsUtilsTest {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val site = SiteModel().apply {
        id = 1
        siteId = 123456L
    }

    @Test
    fun `old Stats reports being opened without the new stats marker`() {
        analyticsTracker.trackStatsAccessed(site, tapSource = "row")

        verify(analyticsTracker).track(
            eq(Stat.STATS_ACCESSED),
            eq(site),
            eq(mapOf<String, Any?>("tap_source" to "row"))
        )
    }

    @Test
    fun `New Stats reports the same open event, marked so the two can be told apart`() {
        analyticsTracker.trackStatsAccessed(site, tapSource = "row", isNewStats = true)

        verify(analyticsTracker).track(
            eq(Stat.STATS_ACCESSED),
            eq(site),
            eq(
                mapOf<String, Any?>(
                    "tap_source" to "row",
                    "new_stats" to "1"
                )
            )
        )
    }

    @Test
    fun `a widget event carries the site the widget is configured for`() {
        analyticsTracker.trackWithWidgetType(Stat.STATS_WIDGET_ADDED, WidgetType.TODAY_VIEWS, site)

        verify(analyticsTracker).track(
            eq(Stat.STATS_WIDGET_ADDED),
            eq(site),
            eq(mapOf<String, Any?>("widget_type" to "today"))
        )
    }

    /** A widget whose configuration is gone: the interaction is still worth more than the site. */
    @Test
    fun `a widget event with no resolvable site still reports`() {
        analyticsTracker.trackMinifiedWidget(Stat.STATS_WIDGET_REMOVED, site = null)

        verify(analyticsTracker).track(
            eq(Stat.STATS_WIDGET_REMOVED),
            isNull<SiteModel>(),
            eq(mapOf<String, Any?>("widget_type" to "minified"))
        )
    }
}
