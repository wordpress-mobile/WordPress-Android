package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.stats.StatsTimeframe

class NewStatsTargetTest {
    @Test
    fun `maps every deep link timeframe onto the closest New Stats target`() {
        val expected = mapOf(
            StatsTimeframe.DAY to NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Today),
            StatsTimeframe.WEEK to NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last7Days),
            StatsTimeframe.MONTH to NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last30Days),
            StatsTimeframe.YEAR to NewStatsTarget(StatsTab.TRAFFIC, StatsPeriod.Last12Months),
            StatsTimeframe.INSIGHTS to NewStatsTarget(StatsTab.INSIGHTS, null),
            StatsTimeframe.SUBSCRIBERS to NewStatsTarget(StatsTab.SUBSCRIBERS, null),
            StatsTimeframe.TRAFFIC to NewStatsTarget(StatsTab.TRAFFIC, null)
        )

        assertThat(StatsTimeframe.entries.associateWith { it.toNewStatsTarget() })
            .isEqualTo(expected)
    }

    @Test
    fun `falls back to traffic with the persisted period when the link names no timeframe`() {
        assertThat(null.toNewStatsTarget()).isEqualTo(NewStatsTarget(StatsTab.TRAFFIC, null))
    }

    @Test
    fun `round-trips every tab through the name the Intent extra carries`() {
        assertThat(StatsTab.entries.associateWith { StatsTab.fromName(it.name) })
            .isEqualTo(StatsTab.entries.associateWith { it })
    }

    @Test
    fun `resolves an unknown or missing tab name to traffic`() {
        assertThat(StatsTab.fromName("not_a_tab")).isEqualTo(StatsTab.TRAFFIC)
        assertThat(StatsTab.fromName(null)).isEqualTo(StatsTab.TRAFFIC)
    }
}
