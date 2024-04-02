package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.STATS_INSIGHTS_VIEWS_VISITORS_TOGGLED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_TOTAL
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val TAP_SOURCE_PROPERTY = "tap_source"
private const val GRANULARITY_PROPERTY = "granularity"
private const val PERIOD_PROPERTY = "period"
private const val DAYS_PROPERTY = "days"
private const val WEEKS_PROPERTY = "weeks"
private const val MONTHS_PROPERTY = "months"
private const val YEARS_PROPERTY = "years"
private const val TYPE = "type"
private const val TYPES = "types"
private const val WIDGET_TYPE = "widget_type"
private const val TODAY_WIDGET_PROPERTY = "today"
private const val WEEKLY_VIEWS_WIDGET_PROPERTY = "weekly_views"
private const val WEEK_TOTALS_WIDGET_PROPERTY = "week_totals"
private const val ALL_TIME_WIDGET_PROPERTY = "all_time"
private const val MINIFIED_WIDGET_PROPERTY = "minified"
private const val CHIP_VIEWS_PROPERTY = "views"
private const val CHIP_VISITORS__PROPERTY = "visitors"

enum class StatsLaunchedFrom(val value: String) {
    QUICK_ACTIONS("quick_actions"),
    TODAY_STATS_CARD("today_stats_card"),
    ROW("row"),
    POSTS("posts"),
    WIDGET("widget"),
    NOTIFICATION("notification"),
    LINK("link"),
    SHORTCUT("shortcut"),
    ACTIVITY_LOG("activity_log"),
}

fun AnalyticsTrackerWrapper.trackStatsAccessed(site: SiteModel, tapSource: String) =
    track(stat = Stat.STATS_ACCESSED, site = site, properties = mutableMapOf(TAP_SOURCE_PROPERTY to tapSource))

fun AnalyticsTrackerWrapper.trackGranular(stat: Stat, granularity: StatsGranularity) =
    track(stat, mapOf(GRANULARITY_PROPERTY to getPropertyByGranularity(granularity)))

fun AnalyticsTrackerWrapper.trackViewsVisitorsChips(position: Int) {
    val property = when (position) {
        0 -> CHIP_VIEWS_PROPERTY
        else -> CHIP_VISITORS__PROPERTY
    }
    this.track(STATS_INSIGHTS_VIEWS_VISITORS_TOGGLED, mapOf(TYPE to property))
}

fun AnalyticsTrackerWrapper.trackWithGranularity(stat: Stat, granularity: StatsGranularity) =
    track(stat, mapOf(PERIOD_PROPERTY to getPropertyByGranularity(granularity)))

private fun getPropertyByGranularity(granularity: StatsGranularity) = when (granularity) {
    StatsGranularity.DAYS -> DAYS_PROPERTY
    StatsGranularity.WEEKS -> WEEKS_PROPERTY
    StatsGranularity.MONTHS -> MONTHS_PROPERTY
    StatsGranularity.YEARS -> YEARS_PROPERTY
}

fun AnalyticsTrackerWrapper.trackWithType(stat: Stat, insightType: InsightType) {
    this.track(stat, mapOf(TYPE to insightType.name))
}

fun AnalyticsTrackerWrapper.trackWithTypes(stat: Stat, insightTypes: Set<InsightType>) {
    this.track(stat, mapOf(TYPES to insightTypes.map { it.name }))
}

fun AnalyticsTrackerWrapper.trackWithWidgetType(stat: Stat, widgetType: WidgetType) {
    val property = when (widgetType) {
        WEEK_VIEWS -> WEEKLY_VIEWS_WIDGET_PROPERTY
        ALL_TIME_VIEWS -> ALL_TIME_WIDGET_PROPERTY
        TODAY_VIEWS -> TODAY_WIDGET_PROPERTY
        WEEK_TOTAL -> WEEK_TOTALS_WIDGET_PROPERTY
    }
    this.track(stat, mapOf(WIDGET_TYPE to property))
}

fun AnalyticsTrackerWrapper.trackMinifiedWidget(stat: Stat) {
    this.track(stat, mapOf(WIDGET_TYPE to MINIFIED_WIDGET_PROPERTY))
}
