package org.wordpress.android.ui.newstats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDate

class StatsPeriodTest {
    @Test
    fun `every preset survives a round trip through its type string`() {
        StatsPeriod.presets().forEach { preset ->
            assertThat(StatsPeriod.fromTypeString(preset.toTypeString())).isEqualTo(preset)
        }
    }

    @Test
    fun `the multi-year presets are offered in the rolling group, coarsest last`() {
        assertThat(StatsPeriod.rollingPresets()).containsExactly(
            StatsPeriod.Last7Days,
            StatsPeriod.Last30Days,
            StatsPeriod.Last12Months,
            StatsPeriod.Last3Years
        )
    }

    @Test
    fun `an unknown or incomplete stored period falls back to Last 7 Days`() {
        assertThat(StatsPeriod.fromTypeString("last_5_years")).isEqualTo(StatsPeriod.Last7Days)
        // A custom range with no stored dates has nothing to restore.
        assertThat(StatsPeriod.fromTypeString("custom")).isEqualTo(StatsPeriod.Last7Days)
    }

    @Test
    fun `a custom range round trips through its stored epoch days`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2026, 9, 16)

        val restored = StatsPeriod.fromTypeString(
            type = StatsPeriod.Custom(start, end).toTypeString(),
            customStartEpochDay = start.toEpochDay(),
            customEndEpochDay = end.toEpochDay()
        )

        assertThat(restored).isEqualTo(StatsPeriod.Custom(start, end))
    }
}
