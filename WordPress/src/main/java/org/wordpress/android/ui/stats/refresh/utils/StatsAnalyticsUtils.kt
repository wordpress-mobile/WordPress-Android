package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.ALL_TIME_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.TODAY_VIEWS
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType.WEEK_VIEWS
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val GRANULARITY_PROPERTY = "granularity"
private const val DAYS_PROPERTY = "days"
private const val WEEKS_PROPERTY = "weeks"
private const val MONTHS_PROPERTY = "months"
private const val YEARS_PROPERTY = "years"
private const val INSIGHTS_PROPERTY = "insights"
private const val DETAIL_PROPERTY = "detail"
private const val ANNUAL_STATS_PROPERTY = "annual_stats"
private const val TYPE = "type"
private const val TYPES = "types"
private const val WIDGET_TYPE = "widget_type"
private const val TODAY_WIDGET_PROPERTY = "today"
private const val WEEKLY_VIEWS_WIDGET_PROPERTY = "weekly_views"
private const val ALL_TIME_WIDGET_PROPERTY = "all_time"
private const val MINIFIED_WIDGET_PROPERTY = "minified"

fun AnalyticsTrackerWrapper.trackGranular(stat: Stat, granularity: StatsGranularity) {
    val property = when (granularity) {
        StatsGranularity.DAYS -> DAYS_PROPERTY
        StatsGranularity.WEEKS -> WEEKS_PROPERTY
        StatsGranularity.MONTHS -> MONTHS_PROPERTY
        StatsGranularity.YEARS -> YEARS_PROPERTY
    }
    this.track(stat, mapOf(GRANULARITY_PROPERTY to property))
}

fun AnalyticsTrackerWrapper.trackWithSection(stat: Stat, section: StatsSection) {
    val property = when (section) {
        StatsSection.DAYS -> DAYS_PROPERTY
        StatsSection.WEEKS -> WEEKS_PROPERTY
        StatsSection.MONTHS -> MONTHS_PROPERTY
        StatsSection.YEARS -> YEARS_PROPERTY
        StatsSection.INSIGHTS -> INSIGHTS_PROPERTY
        StatsSection.DETAIL -> DETAIL_PROPERTY
        StatsSection.ANNUAL_STATS -> ANNUAL_STATS_PROPERTY
    }
    this.track(stat, mapOf(GRANULARITY_PROPERTY to property))
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
    }
    this.track(stat, mapOf(WIDGET_TYPE to property))
}

fun AnalyticsTrackerWrapper.trackMinifiedWidget(stat: Stat) {
    this.track(stat, mapOf(WIDGET_TYPE to MINIFIED_WIDGET_PROPERTY))
}
