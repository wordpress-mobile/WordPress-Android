package org.wordpress.android.ui.newstats.poststats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.newstats.datasource.PostViewsChange
import org.wordpress.android.ui.newstats.datasource.PostViewsDailyView
import org.wordpress.android.ui.newstats.datasource.PostViewsWeek
import java.time.DayOfWeek

class WeeksCalendarTest {
    @Test
    fun `a full week lays out its seven days in order`() {
        val rows = listOf(week(MONDAY_OCT_19, views = List(7) { it.toLong() }))
            .toCalendarRows()

        assertThat(rows).hasSize(1)
        assertThat(rows.first().days).hasSize(7)
        assertThat(rows.first().days.map { it.day }).containsExactly(
            "2026-10-19", "2026-10-20", "2026-10-21", "2026-10-22",
            "2026-10-23", "2026-10-24", "2026-10-25"
        )
        assertThat(rows.first().days.map { it.views })
            .containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L)
    }

    @Test
    fun `the week still running leaves its remaining days empty`() {
        // Oct 26 is a Monday, so a week cut short after Wednesday fills three squares of seven.
        val rows = listOf(week(MONDAY_OCT_26, views = listOf(0L, 0L, 6L)))
            .toCalendarRows()

        val days = rows.first().days
        assertThat(days).hasSize(7)
        assertThat(days.map { it.views })
            .containsExactly(0L, 0L, 6L, 0L, 0L, 0L, 0L)
        // The days that have not happened yet still carry their date, so every square in the row
        // can name itself to a screen reader.
        assertThat(days.drop(3).map { it.day })
            .containsExactly("2026-10-29", "2026-10-30", "2026-10-31", "2026-11-01")
    }

    @Test
    fun `a week that starts mid-week is still placed by weekday`() {
        // An item published on a Thursday gets an oldest week with only four days in it. They
        // belong under Thursday onwards, not under Monday onwards -- so the column order has to
        // come from the full weeks, not from that week's own first day.
        val rows = listOf(
            week(MONDAY_OCT_26, views = listOf(5L)),
            week(MONDAY_OCT_19, views = List(7) { 1L }),
            week(MONDAY_OCT_12, views = List(7) { 1L }),
            PostViewsWeek(
                startDay = THURSDAY_OCT_8,
                endDay = "2026-10-11",
                total = 10L,
                change = PostViewsChange.None,
                days = listOf(
                    PostViewsDailyView(THURSDAY_OCT_8, 1L),
                    PostViewsDailyView("2026-10-09", 2L),
                    PostViewsDailyView("2026-10-10", 3L),
                    PostViewsDailyView("2026-10-11", 4L)
                )
            )
        ).toCalendarRows()

        assertThat(rows.last().weekStart).isEqualTo(DayOfWeek.MONDAY)
        assertThat(rows.last().days.map { it.views })
            .containsExactly(0L, 0L, 0L, 1L, 2L, 3L, 4L)
        assertThat(rows.last().days.first().day).isEqualTo("2026-10-05")
    }

    @Test
    fun `the first weekday is read off the data`() {
        val rows = listOf(week(MONDAY_OCT_26, views = listOf(1L)))
            .toCalendarRows()

        assertThat(rows.first().weekStart).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `weeks with no days at all are dropped`() {
        val rows = listOf(
            week(MONDAY_OCT_19, views = emptyList()),
            week(MONDAY_OCT_26, views = listOf(1L))
        ).toCalendarRows()

        assertThat(rows).hasSize(1)
    }

    @Test
    fun `shading is bucketed by quarters of the busiest day`() {
        val busiest = 100L

        assertThat(IntensityLevel.of(0L, busiest))
            .isEqualTo(IntensityLevel.NONE)
        assertThat(IntensityLevel.of(1L, busiest))
            .isEqualTo(IntensityLevel.LOW)
        assertThat(IntensityLevel.of(25L, busiest))
            .isEqualTo(IntensityLevel.LOW)
        assertThat(IntensityLevel.of(26L, busiest))
            .isEqualTo(IntensityLevel.MEDIUM)
        assertThat(IntensityLevel.of(51L, busiest))
            .isEqualTo(IntensityLevel.HIGH)
        assertThat(IntensityLevel.of(76L, busiest))
            .isEqualTo(IntensityLevel.VERY_HIGH)
        assertThat(IntensityLevel.of(busiest, busiest))
            .isEqualTo(IntensityLevel.VERY_HIGH)
    }

    @Test
    fun `a week nobody read is shaded as empty rather than as its own busiest day`() {
        assertThat(IntensityLevel.of(0L, 0L))
            .isEqualTo(IntensityLevel.NONE)
    }

    private fun week(startDay: String, views: List<Long>) = PostViewsWeek(
        startDay = startDay,
        endDay = startDay,
        total = views.sum(),
        change = PostViewsChange.None,
        days = views.mapIndexed { index, count ->
            PostViewsDailyView(
                day = startDay.plusDays(index),
                views = count
            )
        }
    )

    private fun String.plusDays(days: Int) =
        java.time.LocalDate.parse(this).plusDays(days.toLong()).toString()

    companion object {
        private const val THURSDAY_OCT_8 = "2026-10-08"
        private const val MONDAY_OCT_12 = "2026-10-12"
        private const val MONDAY_OCT_19 = "2026-10-19"
        private const val MONDAY_OCT_26 = "2026-10-26"
    }
}
