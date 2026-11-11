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
private const val NEW_STATS_PROPERTY = "new_stats"
private const val GRANULARITY_PROPERTY = "granularity"
private const val PERIOD_PROPERTY = "period"
private const val HOURS_PROPERTY = "hours"
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
    STATS_TOGGLE("stats_toggle"),
}

/**
 * Both Stats screens report this under the same name, so the open counts stay comparable for the
 * length of the rollout. [isNewStats] is what tells them apart: it is set only by New Stats, and
 * matches how iOS marks the same event (`new_stats` = "1").
 */
fun AnalyticsTrackerWrapper.trackStatsAccessed(
    site: SiteModel,
    tapSource: String,
    isNewStats: Boolean = false
) = track(
    stat = Stat.STATS_ACCESSED,
    site = site,
    properties = buildMap {
        put(TAP_SOURCE_PROPERTY, tapSource)
        if (isNewStats) put(NEW_STATS_PROPERTY, "1")
    }
)

fun AnalyticsTrackerWrapper.trackGranular(stat: Stat, site: SiteModel?, granularity: StatsGranularity) =
    track(stat, site, mapOf(GRANULARITY_PROPERTY to getPropertyByGranularity(granularity)))

fun AnalyticsTrackerWrapper.trackViewsVisitorsChips(site: SiteModel?, position: Int) {
    val property = when (position) {
        0 -> CHIP_VIEWS_PROPERTY
        else -> CHIP_VISITORS__PROPERTY
    }
    this.track(STATS_INSIGHTS_VIEWS_VISITORS_TOGGLED, site, mapOf(TYPE to property))
}

fun AnalyticsTrackerWrapper.trackWithGranularity(stat: Stat, site: SiteModel?, granularity: StatsGranularity) =
    track(stat, site, mapOf(PERIOD_PROPERTY to getPropertyByGranularity(granularity)))

private fun getPropertyByGranularity(granularity: StatsGranularity) = when (granularity) {
    StatsGranularity.HOURS -> HOURS_PROPERTY
    StatsGranularity.DAYS -> DAYS_PROPERTY
    StatsGranularity.WEEKS -> WEEKS_PROPERTY
    StatsGranularity.MONTHS -> MONTHS_PROPERTY
    StatsGranularity.YEARS -> YEARS_PROPERTY
}

fun AnalyticsTrackerWrapper.trackWithType(stat: Stat, site: SiteModel?, insightType: InsightType) {
    this.track(stat, site, mapOf(TYPE to insightType.name))
}

fun AnalyticsTrackerWrapper.trackWithTypes(stat: Stat, site: SiteModel?, insightTypes: Set<InsightType>) {
    this.track(stat, site, mapOf(TYPES to insightTypes.map { it.name }))
}

/**
 * [site] is the site the widget is configured for, read back from the widget's own configuration
 * rather than from a stats provider - these events fire outside any stats screen, from the
 * configure flow and from the launcher's delete callback (CMM-2273). Null when the configuration is
 * gone or names a site that is no longer installed, in which case the event still reports, without
 * the site properties.
 */
fun AnalyticsTrackerWrapper.trackWithWidgetType(stat: Stat, widgetType: WidgetType, site: SiteModel?) {
    val property = when (widgetType) {
        WEEK_VIEWS -> WEEKLY_VIEWS_WIDGET_PROPERTY
        ALL_TIME_VIEWS -> ALL_TIME_WIDGET_PROPERTY
        TODAY_VIEWS -> TODAY_WIDGET_PROPERTY
        WEEK_TOTAL -> WEEK_TOTALS_WIDGET_PROPERTY
    }
    this.track(stat, site, mapOf(WIDGET_TYPE to property))
}

/** See [trackWithWidgetType] for where [site] comes from and why it can be null. */
fun AnalyticsTrackerWrapper.trackMinifiedWidget(stat: Stat, site: SiteModel?) {
    this.track(stat, site, mapOf(WIDGET_TYPE to MINIFIED_WIDGET_PROPERTY))
}
