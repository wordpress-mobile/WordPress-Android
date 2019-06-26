package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel.StatsSection
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
