package org.wordpress.android.ui.stats.refresh.utils

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

private const val GRANULARITY_PROPERTY = "granularity"
private const val DAYS_PROPERTY = "days"
private const val WEEKS_PROPERTY = "weeks"
private const val MONTHS_PROPERTY = "months"
private const val YEARS_PROPERTY = "years"
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

fun AnalyticsTrackerWrapper.trackWithType(stat: Stat, insightType: InsightType) {
    this.track(stat, mapOf(TYPE to insightType.name))
}
