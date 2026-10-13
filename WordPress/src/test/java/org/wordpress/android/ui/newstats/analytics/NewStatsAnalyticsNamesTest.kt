package org.wordpress.android.ui.newstats.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.newstats.InsightsCardType
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.StatsTab
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.devices.DeviceType
import org.wordpress.android.ui.newstats.locations.LocationType
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardType
import org.wordpress.android.ui.newstats.subscribers.subscribersgraph.SubscribersGraphTab
import org.wordpress.android.ui.newstats.utm.UtmCategory
import org.wordpress.android.ui.newstats.viewsstats.ChartType
import org.wordpress.android.ui.newstats.viewsstats.StatsMetric
import java.time.LocalDate

/**
 * Pins every reported name to the exact string iOS already sends, because a dashboard reads both
 * platforms as one series: renaming an enum entry, or adding one without naming it here, has to
 * fail locally rather than quietly split a metric in two.
 *
 * Each case asserts the whole mapping at once, so a new enum entry fails even when nothing else
 * about it changed.
 */
class NewStatsAnalyticsNamesTest {
    @Test
    fun `tabs report the iOS tab names`() {
        assertThat(StatsTab.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                StatsTab.TRAFFIC to "traffic",
                StatsTab.INSIGHTS to "insights",
                StatsTab.SUBSCRIBERS to "subscribers"
            )
        )
    }

    @Test
    fun `periods report the iOS preset keys`() {
        val custom = StatsPeriod.Custom(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        val periods = StatsPeriod.presets() + custom

        assertThat(periods.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                StatsPeriod.Today to "today",
                StatsPeriod.Last7Days to "last_7_days",
                StatsPeriod.Last30Days to "last_30_days",
                StatsPeriod.Last12Months to "last_12_months",
                StatsPeriod.ThisWeek to "this_week",
                StatsPeriod.ThisMonth to "this_month",
                StatsPeriod.ThisYear to "this_year",
                custom to "custom"
            )
        )
    }

    @Test
    fun `a custom range reports its type, never its dates`() {
        val january = StatsPeriod.Custom(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        val february = StatsPeriod.Custom(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))

        assertThat(january.analyticsName).isEqualTo(february.analyticsName)
    }

    @Test
    fun `metrics report the iOS metric names`() {
        assertThat(StatsMetric.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                StatsMetric.VIEWS to "views",
                StatsMetric.VISITORS to "visitors",
                StatsMetric.LIKES to "likes",
                StatsMetric.COMMENTS to "comments",
                StatsMetric.POSTS to "posts"
            )
        )
    }

    @Test
    fun `chart types report the iOS chart type names`() {
        assertThat(ChartType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                ChartType.LINE to "line",
                // iOS calls the bar chart `columns`; this deliberately differs from the enum's
                // own persisted storage key ("bar"), which cannot be renamed to match.
                ChartType.BAR to "columns"
            )
        )
    }

    @Test
    fun `units report the iOS granularity names, which are singular`() {
        assertThat(StatsUnit.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                StatsUnit.HOUR to "hour",
                StatsUnit.DAY to "day",
                StatsUnit.WEEK to "week",
                StatsUnit.MONTH to "month",
                StatsUnit.YEAR to "year"
            )
        )
    }

    /** `external_links` and `videos` are iOS's names for Clicks and Videos - see the mapping. */
    @Test
    fun `traffic cards report the iOS top list item names`() {
        assertThat(StatsCardType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                StatsCardType.TODAYS_STATS to "today",
                StatsCardType.VIEWS_STATS to "chart",
                StatsCardType.MOST_VIEWED_POSTS_AND_PAGES to "posts_and_pages",
                StatsCardType.MOST_VIEWED_REFERRERS to "referrers",
                StatsCardType.LOCATIONS to "locations",
                StatsCardType.AUTHORS to "authors",
                StatsCardType.CLICKS to "external_links",
                StatsCardType.SEARCH_TERMS to "search_terms",
                StatsCardType.VIDEO_PLAYS to "videos",
                StatsCardType.FILE_DOWNLOADS to "file_downloads",
                StatsCardType.DEVICES to "devices",
                StatsCardType.UTM to "utm"
            )
        )
    }

    /** iOS has one card class per kind, so every top list is `top_list` there. */
    @Test
    fun `traffic cards report the iOS card kind`() {
        assertThat(StatsCardType.entries.associateWith { it.cardAnalyticsName }).isEqualTo(
            mapOf(
                StatsCardType.TODAYS_STATS to "today",
                StatsCardType.VIEWS_STATS to "chart",
                StatsCardType.MOST_VIEWED_POSTS_AND_PAGES to "top_list",
                StatsCardType.MOST_VIEWED_REFERRERS to "top_list",
                StatsCardType.LOCATIONS to "top_list",
                StatsCardType.AUTHORS to "top_list",
                StatsCardType.CLICKS to "top_list",
                StatsCardType.SEARCH_TERMS to "top_list",
                StatsCardType.VIDEO_PLAYS to "top_list",
                StatsCardType.FILE_DOWNLOADS to "top_list",
                StatsCardType.DEVICES to "top_list",
                StatsCardType.UTM to "top_list"
            )
        )
    }

    @Test
    fun `insights cards report snake_case names`() {
        assertThat(InsightsCardType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                InsightsCardType.YEAR_IN_REVIEW to "year_in_review",
                InsightsCardType.ALL_TIME_STATS to "all_time_stats",
                InsightsCardType.LATEST_POST to "latest_post",
                InsightsCardType.MOST_POPULAR_DAY to "most_popular_day",
                InsightsCardType.MOST_POPULAR_TIME to "most_popular_time",
                InsightsCardType.TAGS_AND_CATEGORIES to "tags_and_categories"
            )
        )
    }

    @Test
    fun `subscribers cards report snake_case names`() {
        assertThat(SubscribersCardType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                SubscribersCardType.ALL_TIME_SUBSCRIBERS to "all_time_subscribers",
                SubscribersCardType.SUBSCRIBERS_GRAPH to "subscribers_graph",
                SubscribersCardType.SUBSCRIBERS_LIST to "subscribers_list",
                SubscribersCardType.EMAILS to "emails"
            )
        )
    }

    @Test
    fun `the subscribers chart ranges report their granularity`() {
        assertThat(SubscribersGraphTab.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                SubscribersGraphTab.DAYS to "day",
                SubscribersGraphTab.WEEKS to "week",
                SubscribersGraphTab.MONTHS to "month",
                SubscribersGraphTab.YEARS to "year"
            )
        )
    }

    @Test
    fun `location levels report the iOS plural names`() {
        assertThat(LocationType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                LocationType.COUNTRIES to "countries",
                LocationType.REGIONS to "regions",
                LocationType.CITIES to "cities"
            )
        )
    }

    @Test
    fun `device breakdowns report the iOS breakdown names`() {
        assertThat(DeviceType.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                DeviceType.SCREENSIZE to "screensize",
                DeviceType.BROWSER to "browser",
                DeviceType.PLATFORM to "platform"
            )
        )
    }

    /** camelCase on purpose: these are iOS's raw values, already in use. */
    @Test
    fun `utm groupings report the iOS camelCase names`() {
        assertThat(UtmCategory.entries.associateWith { it.analyticsName }).isEqualTo(
            mapOf(
                UtmCategory.SOURCE_MEDIUM to "sourceMedium",
                UtmCategory.CAMPAIGN_SOURCE_MEDIUM to "campaignSourceMedium",
                UtmCategory.SOURCE to "source",
                UtmCategory.MEDIUM to "medium",
                UtmCategory.CAMPAIGN to "campaign"
            )
        )
    }
}
