package org.wordpress.android.ui.stats.refresh.lists.widget

import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_WIDGET_REMOVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.MinifiedWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.utils.WidgetUtils
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekViewsWidgetUpdater
import org.wordpress.android.ui.stats.refresh.utils.StatsUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.StatsTrafficSubscribersTabsFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider

/**
 * Removing a widget is the one place the site has to be read before the work is done: `delete`
 * clears the widget's configuration, and that configuration is the only record of which site the
 * widget belonged to. Tracking after the clear would report every removal site-less (CMM-2273).
 */
@RunWith(MockitoJUnitRunner::class)
class WidgetRemovalTrackingTest {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val site = SiteModel().apply {
        id = 1
        siteId = SITE_ID
    }

    @Test
    fun `removing the views widget reports the site it was configured for`() {
        stubConfiguredSite()

        viewsWidgetUpdater().delete(APP_WIDGET_ID)

        verifyTrackedWithSiteBeforeTheConfigurationIsCleared("weekly_views")
    }

    @Test
    fun `removing the all time widget reports the site it was configured for`() {
        stubConfiguredSite()

        allTimeWidgetUpdater().delete(APP_WIDGET_ID)

        verifyTrackedWithSiteBeforeTheConfigurationIsCleared("all_time")
    }

    @Test
    fun `removing the today widget reports the site it was configured for`() {
        stubConfiguredSite()

        todayWidgetUpdater().delete(APP_WIDGET_ID)

        verifyTrackedWithSiteBeforeTheConfigurationIsCleared("today")
    }

    @Test
    fun `removing the week totals widget reports the site it was configured for`() {
        stubConfiguredSite()

        weekViewsWidgetUpdater().delete(APP_WIDGET_ID)

        verifyTrackedWithSiteBeforeTheConfigurationIsCleared("week_totals")
    }

    @Test
    fun `removing the minified widget reports the site it was configured for`() {
        stubConfiguredSite()

        minifiedWidgetUpdater().delete(APP_WIDGET_ID)

        verifyTrackedWithSiteBeforeTheConfigurationIsCleared("minified")
    }

    @Test
    fun `a widget whose site is no longer installed still reports its removal`() {
        whenever(appPrefsWrapper.getAppWidgetSiteId(APP_WIDGET_ID)).thenReturn(SITE_ID)
        whenever(siteStore.getSiteBySiteId(SITE_ID)).thenReturn(null)

        viewsWidgetUpdater().delete(APP_WIDGET_ID)

        verify(analyticsTrackerWrapper).track(
            eq(STATS_WIDGET_REMOVED),
            eq<SiteModel?>(null),
            eq(mapOf<String, Any?>("widget_type" to "weekly_views"))
        )
    }

    private fun stubConfiguredSite() {
        whenever(appPrefsWrapper.getAppWidgetSiteId(APP_WIDGET_ID)).thenReturn(SITE_ID)
        whenever(siteStore.getSiteBySiteId(SITE_ID)).thenReturn(site)
    }

    private fun verifyTrackedWithSiteBeforeTheConfigurationIsCleared(widgetType: String) {
        inOrder(analyticsTrackerWrapper, appPrefsWrapper) {
            verify(analyticsTrackerWrapper).track(
                eq(STATS_WIDGET_REMOVED),
                eq(site),
                eq(mapOf<String, Any?>("widget_type" to widgetType))
            )
            verify(appPrefsWrapper).removeAppWidgetSiteId(APP_WIDGET_ID)
        }
    }

    private fun viewsWidgetUpdater() = ViewsWidgetUpdater(
        appPrefsWrapper,
        siteStore,
        mock<AccountStore>(),
        mock<NetworkUtilsWrapper>(),
        mock<ResourceProvider>(),
        mock<WidgetUtils>(),
        analyticsTrackerWrapper
    )

    private fun allTimeWidgetUpdater() = AllTimeWidgetUpdater(
        appPrefsWrapper,
        siteStore,
        mock<AccountStore>(),
        mock<NetworkUtilsWrapper>(),
        mock<ResourceProvider>(),
        mock<WidgetUtils>(),
        analyticsTrackerWrapper
    )

    private fun todayWidgetUpdater() = TodayWidgetUpdater(
        appPrefsWrapper,
        siteStore,
        mock<AccountStore>(),
        mock<NetworkUtilsWrapper>(),
        mock<ResourceProvider>(),
        mock<WidgetUtils>(),
        analyticsTrackerWrapper,
        mock<StatsTrafficSubscribersTabsFeatureConfig>()
    )

    private fun weekViewsWidgetUpdater() = WeekViewsWidgetUpdater(
        appPrefsWrapper,
        siteStore,
        mock<AccountStore>(),
        mock<NetworkUtilsWrapper>(),
        mock<ResourceProvider>(),
        mock<WidgetUtils>(),
        analyticsTrackerWrapper,
        mock<StatsTrafficSubscribersTabsFeatureConfig>()
    )

    private fun minifiedWidgetUpdater() = MinifiedWidgetUpdater(
        Dispatchers.Unconfined,
        appPrefsWrapper,
        siteStore,
        mock<AccountStore>(),
        mock<NetworkUtilsWrapper>(),
        mock<ResourceProvider>(),
        mock<StatsUtils>(),
        mock<TodayInsightsStore>(),
        mock<WidgetUtils>(),
        analyticsTrackerWrapper,
        mock<StatsTrafficSubscribersTabsFeatureConfig>()
    )

    companion object {
        private const val APP_WIDGET_ID = 7
        private const val SITE_ID = 123456L
    }
}
