package org.wordpress.android.ui.newstats.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.InsightsCardType
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.StatsTab
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.CardMoveDirection
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker.NavigationDirection
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.devices.DeviceType
import org.wordpress.android.ui.newstats.locations.LocationType
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardType
import org.wordpress.android.ui.newstats.subscribers.subscribersgraph.SubscribersGraphTab
import org.wordpress.android.ui.newstats.utm.UtmCategory
import org.wordpress.android.ui.newstats.viewsstats.ChartType
import org.wordpress.android.ui.newstats.viewsstats.StatsMetric
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.time.LocalDate

@RunWith(MockitoJUnitRunner::class)
class NewStatsTrackerTest {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    private val site = SiteModel().apply {
        id = 1
        siteId = 123456L
    }

    private lateinit var tracker: NewStatsTracker

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        tracker = NewStatsTracker(analyticsTracker, selectedSiteRepository)
    }

    // region The site

    @Test
    fun `every event carries the selected site`() {
        tracker.trackMainScreenShown()

        verify(analyticsTracker).track(eq(Stat.JETPACK_STATS_MAIN_SCREEN_SHOWN), eq(site), isNull())
    }

    @Test
    fun `an event with properties still carries the site`() {
        tracker.trackTabShown(StatsTab.INSIGHTS)

        verify(analyticsTracker).track(
            eq(Stat.JETPACK_STATS_INSIGHTS_TAB_SHOWN),
            eq(site),
            isNull()
        )
    }

    @Test
    fun `without a selected site the event still reports`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        tracker.trackMainScreenShown()

        verify(analyticsTracker).track(
            eq(Stat.JETPACK_STATS_MAIN_SCREEN_SHOWN),
            isNull<SiteModel>(),
            isNull()
        )
    }

    // endregion

    // region Screens

    @Test
    fun `each tab reports its own shown event`() {
        tracker.trackTabShown(StatsTab.TRAFFIC)
        tracker.trackTabShown(StatsTab.INSIGHTS)
        tracker.trackTabShown(StatsTab.SUBSCRIBERS)

        verify(analyticsTracker).track(eq(Stat.JETPACK_STATS_TRAFFIC_TAB_SHOWN), eq(site), isNull())
        verify(analyticsTracker).track(
            eq(Stat.JETPACK_STATS_INSIGHTS_TAB_SHOWN),
            eq(site),
            isNull()
        )
        verify(analyticsTracker).track(
            eq(Stat.JETPACK_STATS_SUBSCRIBERS_TAB_SHOWN),
            eq(site),
            isNull()
        )
    }

    @Test
    fun `a tab change reports both the new tab and the one left`() {
        tracker.trackTabSelected(from = StatsTab.TRAFFIC, to = StatsTab.SUBSCRIBERS)

        assertThat(propertiesOf(Stat.JETPACK_STATS_TAB_SELECTED)).isEqualTo(
            mapOf(
                "tab_name" to "subscribers",
                "previous_tab" to "traffic"
            )
        )
    }

    @Test
    fun `a detail screen reports which screen it is, for all three card families`() {
        tracker.trackDetailScreenShown(StatsCardType.AUTHORS)
        assertThat(propertiesOf(Stat.JETPACK_STATS_DETAIL_SCREEN_SHOWN))
            .isEqualTo(mapOf("screen" to "authors"))

        tracker.trackDetailScreenShown(InsightsCardType.YEAR_IN_REVIEW)
        assertThat(propertiesOf(Stat.JETPACK_STATS_DETAIL_SCREEN_SHOWN, invocation = 1))
            .isEqualTo(mapOf("screen" to "year_in_review"))

        tracker.trackDetailScreenShown(SubscribersCardType.EMAILS)
        assertThat(propertiesOf(Stat.JETPACK_STATS_DETAIL_SCREEN_SHOWN, invocation = 2))
            .isEqualTo(mapOf("screen" to "emails"))
    }

    // endregion

    // region Date range

    @Test
    fun `a preset reports which preset`() {
        tracker.trackDateRangePresetSelected(StatsPeriod.Last30Days)

        assertThat(propertiesOf(Stat.JETPACK_STATS_DATE_RANGE_PRESET_SELECTED))
            .isEqualTo(mapOf("selected_preset" to "last_30_days"))
    }

    @Test
    fun `a custom range reports plain ISO dates`() {
        tracker.trackCustomDateRangeSelected(
            startDate = LocalDate.of(2026, 3, 1),
            endDate = LocalDate.of(2026, 3, 9)
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_CUSTOM_DATE_RANGE_SELECTED)).isEqualTo(
            mapOf(
                "start_date" to "2026-03-01",
                "end_date" to "2026-03-09"
            )
        )
    }

    @Test
    fun `date navigation reports the direction and the range being left`() {
        tracker.trackDateNavigationButtonTapped(
            direction = NavigationDirection.PREVIOUS,
            currentPeriod = StatsPeriod.ThisWeek
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_DATE_NAVIGATION_BUTTON_TAPPED)).isEqualTo(
            mapOf(
                "direction" to "previous",
                "current_period_type" to "this_week"
            )
        )
    }

    // endregion

    // region Cards

    @Test
    fun `adding and removing a card report the card type`() {
        tracker.trackCardAdded(StatsCardType.CLICKS)
        assertThat(propertiesOf(Stat.JETPACK_STATS_CARD_ADDED))
            .isEqualTo(mapOf("card_type" to "external_links"))

        tracker.trackCardRemoved(InsightsCardType.LATEST_POST)
        assertThat(propertiesOf(Stat.JETPACK_STATS_CARD_REMOVED))
            .isEqualTo(mapOf("card_type" to "latest_post"))
    }

    @Test
    fun `moving a card reports the menu item used and where it landed`() {
        tracker.trackCardMoved(
            cardType = SubscribersCardType.EMAILS,
            direction = CardMoveDirection.TOP,
            fromIndex = 3,
            toIndex = 0
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_CARD_MOVED)).isEqualTo(
            mapOf(
                "card_type" to "emails",
                "action" to "move_to_top",
                "from_index" to 3,
                "to_index" to 0
            )
        )
    }

    // endregion

    // region Chart

    @Test
    fun `a chart type change reports both types`() {
        tracker.trackChartTypeChanged(from = ChartType.BAR, to = ChartType.LINE)

        assertThat(propertiesOf(Stat.JETPACK_STATS_CHART_TYPE_CHANGED)).isEqualTo(
            mapOf(
                "from_type" to "bar",
                "to_type" to "line"
            )
        )
    }

    @Test
    fun `a metric selection reports the metric`() {
        tracker.trackChartMetricSelected(StatsMetric.VISITORS)

        assertThat(propertiesOf(Stat.JETPACK_STATS_CHART_METRIC_SELECTED))
            .isEqualTo(mapOf("metric" to "visitors"))
    }

    @Test
    fun `a tapped bar reports its bucket width, metric and total`() {
        tracker.trackChartBarSelected(
            metric = StatsMetric.LIKES,
            unit = StatsUnit.WEEK,
            value = 42L
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_CHART_BAR_SELECTED)).isEqualTo(
            mapOf(
                "from_granularity" to "week",
                "metric" to "likes",
                "value" to 42L
            )
        )
    }

    // endregion

    // region Lists

    @Test
    fun `a tapped row reports which list it came from`() {
        tracker.trackTopListItemTapped(StatsCardType.MOST_VIEWED_REFERRERS)

        assertThat(propertiesOf(Stat.JETPACK_STATS_TOP_LIST_ITEM_TAPPED))
            .isEqualTo(mapOf("item_type" to "referrers"))
    }

    @Test
    fun `a tapped row in a ranked list also reports the ranking metric`() {
        tracker.trackTopListItemTapped(
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES,
            metric = StatsMetric.VIEWS
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_TOP_LIST_ITEM_TAPPED)).isEqualTo(
            mapOf(
                "item_type" to "posts_and_pages",
                "metric" to "views"
            )
        )
    }

    @Test
    fun `the list selectors report what they moved from and to`() {
        tracker.trackLocationLevelChanged(from = LocationType.COUNTRIES, to = LocationType.CITIES)
        assertThat(propertiesOf(Stat.JETPACK_STATS_LOCATION_LEVEL_CHANGED)).isEqualTo(
            mapOf(
                "from_level" to "countries",
                "to_level" to "cities"
            )
        )

        tracker.trackDeviceBreakdownChanged(from = DeviceType.BROWSER, to = DeviceType.PLATFORM)
        assertThat(propertiesOf(Stat.JETPACK_STATS_DEVICE_BREAKDOWN_CHANGED)).isEqualTo(
            mapOf(
                "from_breakdown" to "browser",
                "to_breakdown" to "platform"
            )
        )

        tracker.trackUtmParamGroupingChanged(from = UtmCategory.SOURCE, to = UtmCategory.CAMPAIGN)
        assertThat(propertiesOf(Stat.JETPACK_STATS_UTM_PARAM_GROUPING_CHANGED)).isEqualTo(
            mapOf(
                "from_grouping" to "source",
                "to_grouping" to "campaign"
            )
        )
    }

    @Test
    fun `the subscribers chart range reports what it moved from and to`() {
        tracker.trackSubscribersChartRangeChanged(
            from = SubscribersGraphTab.DAYS,
            to = SubscribersGraphTab.YEARS
        )

        assertThat(propertiesOf(Stat.JETPACK_STATS_SUBSCRIBERS_CHART_RANGE_CHANGED)).isEqualTo(
            mapOf(
                "from_granularity" to "day",
                "to_granularity" to "year"
            )
        )
    }

    // endregion

    /**
     * The properties of the [invocation]-th event reported under [stat]. A fresh captor per call,
     * so the index counts only this stat's events - it matters for the cases that report the same
     * event more than once.
     */
    private fun propertiesOf(stat: Stat, invocation: Int = 0): Map<String, Any?>? {
        val captor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsTracker, atLeastOnce()).track(eq(stat), any(), captor.capture())
        return captor.allValues[invocation]
    }
}
