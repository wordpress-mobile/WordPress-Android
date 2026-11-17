package org.wordpress.android.ui.newstats.poststats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.newstats.datasource.PostViewsYear
import org.wordpress.android.ui.newstats.datasource.PostViewsYearAverage

class MonthsCalendarTest {
    @Test
    fun `a year is drawn whole, January to December`() {
        val blocks = listOf(
            year("2026", months = mapOf(1 to 131L, 10 to 42L))
        ).asMonthlyTotals().toCalendarYears(currentYear = 2026)

        assertThat(blocks).hasSize(1)
        assertThat(blocks.first().months).hasSize(12)
        assertThat(blocks.first().months.map { it.month })
            .containsExactlyElementsOf(1..12)
        assertThat(blocks.first().months.map { it.views })
            .containsExactly(131L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 42L, 0L, 0L)
    }

    @Test
    fun `the current year is shown even when the API sent nothing for it`() {
        val blocks = listOf(
            year("2024", months = mapOf(3 to 7L))
        ).asMonthlyTotals().toCalendarYears(currentYear = 2026)

        assertThat(blocks.map { it.label }).containsExactly("2026", "2024")
        assertThat(blocks.first().months.map { it.views }).allMatch { it == 0L }
    }

    @Test
    fun `the current year is not duplicated when the API did send it`() {
        val blocks = listOf(
            year("2026", months = mapOf(1 to 5L)),
            year("2025", months = mapOf(1 to 3L))
        ).asMonthlyTotals().toCalendarYears(currentYear = 2026)

        assertThat(blocks.map { it.label }).containsExactly("2026", "2025")
        assertThat(blocks.first().months.first().views).isEqualTo(5L)
    }

    @Test
    fun `years run most recent first`() {
        val blocks = listOf(
            year("2024", months = emptyMap()),
            year("2026", months = emptyMap()),
            year("2025", months = emptyMap())
        ).asMonthlyTotals().toCalendarYears(currentYear = 2026)

        assertThat(blocks.map { it.label })
            .containsExactly("2026", "2025", "2024")
    }

    @Test
    fun `an item with no view history at all gets no grid`() {
        assertThat(
            emptyList<PostViewsYear>().asMonthlyTotals()
                .toCalendarYears(currentYear = 2026)
        ).isEmpty()
    }

    @Test
    fun `the averages grid is built from the same monthly shape`() {
        val blocks = listOf(
            PostViewsYearAverage(
                year = "2026",
                overall = 1L,
                months = mapOf(6 to 42L, 7 to 4L)
            )
        ).asMonthlyAverages().toCalendarYears(currentYear = 2026)

        assertThat(blocks.first().months.map { it.views })
            .containsExactly(0L, 0L, 0L, 0L, 0L, 42L, 4L, 0L, 0L, 0L, 0L, 0L)
    }

    private fun year(year: String, months: Map<Int, Long>) = PostViewsYear(
        year = year,
        total = months.values.sum(),
        months = months
    )
}
