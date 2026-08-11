package org.wordpress.android.ui.newstats

import org.wordpress.android.ui.stats.StatsTimeframe

/**
 * A null [period] leaves the per-site persisted period in place, which is what the tabs with no
 * period selector and the links that name no timeframe want.
 */
data class NewStatsTarget(val tab: StatsTab, val period: StatsPeriod?)

/**
 * Maps the [StatsTimeframe] carried by an old-stats deep link onto the closest New Stats target.
 * Note [StatsTimeframe.WEEK]'s seven days and [StatsTimeframe.MONTH]'s thirty are the nearest New
 * Stats offers - it has no calendar-aligned periods - and nothing maps to
 * [StatsPeriod.Last6Months].
 */
fun StatsTimeframe?.toNewStatsTarget(): NewStatsTarget = when (this) {
    StatsTimeframe.DAY -> NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Today)
    StatsTimeframe.WEEK -> NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last7Days)
    StatsTimeframe.MONTH -> NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last30Days)
    StatsTimeframe.YEAR -> NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last12Months)
    StatsTimeframe.INSIGHTS -> NewStatsTarget(StatsTab.INSIGHTS, null)
    StatsTimeframe.SUBSCRIBERS -> NewStatsTarget(StatsTab.SUBSCRIBERS, null)
    StatsTimeframe.TRAFFIC, null -> NewStatsTarget(StatsTab.TRAFFIC, null)
}
