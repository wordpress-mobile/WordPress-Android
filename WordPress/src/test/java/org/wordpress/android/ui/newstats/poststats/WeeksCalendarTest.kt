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
            .toCalendarRows(DayOfWeek.MONDAY)

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
            .toCalendarRows(DayOfWeek.MONDAY)

        val days = rows.first().days
        assertThat(days).hasSize(7)
        assertThat(days.map { it.views })
            .containsExactly(0L, 0L, 6L, 0L, 0L, 0L, 0L)
        // The days that have not happened yet still carry their date, so every square in the row
        // can name itself to a screen reader, and they are drawn rather than left blank: the item
        // has had no views on them, which is what a quiet square says.
        assertThat(days.drop(3).map { it.day })
            .containsExactly("2026-10-29", "2026-10-30", "2026-10-31", "2026-11-01")
        assertThat(days).allMatch { it.isInWindow }
    }

    @Test
    fun `the newest week is the first row`() {
        val rows = listOf(
            week(MONDAY_OCT_26, views = listOf(1L)),
            week(MONDAY_OCT_19, views = List(7) { 1L }),
            week(MONDAY_OCT_12, views = List(7) { 1L })
        ).toCalendarRows(DayOfWeek.MONDAY)

        assertThat(rows.map { it.days.first().day })
            .containsExactly("2026-10-26", "2026-10-19", "2026-10-12")
    }

    @Test
    fun `a week that starts mid-week is still placed by weekday`() {
        // An item published on a Thursday gets an oldest week with only four days in it. They
        // belong under Thursday onwards, not under Monday onwards.
        val rows = listOf(
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
        ).toCalendarRows(DayOfWeek.MONDAY)

        val oldest = rows.last()
        assertThat(oldest.days.map { it.views })
            .containsExactly(0L, 0L, 0L, 1L, 2L, 3L, 4L)
        // The row is named for the Monday it starts on, not for the day the item went up.
        assertThat(oldest.days.first().day).isEqualTo("2026-10-05")
        // Monday to Wednesday are before anything we were told about, so they are left blank
        // rather than drawn as days nobody read the item on.
        assertThat(oldest.days.map { it.isInWindow })
            .containsExactly(false, false, false, true, true, true, true)
    }

    @Test
    fun `a Sunday-first locale starts its rows on Sunday`() {
        // The API buckets its weeks from Monday wherever the reader is. A reader whose week starts
        // on Sunday gets rows that start on Sunday, with each day still under its own weekday.
        val rows = listOf(
            week(MONDAY_OCT_19, views = List(7) { it.toLong() })
        ).toCalendarRows(DayOfWeek.SUNDAY)

        // Mon 19 - Sun 25 spans two Sunday-first weeks: Oct 18-24 and Oct 25-31.
        assertThat(rows.map { it.days.first().day })
            .containsExactly("2026-10-25", "2026-10-18")

        val oldest = rows.last()
        assertThat(oldest.days.map { it.day }).containsExactly(
            "2026-10-18", "2026-10-19", "2026-10-20", "2026-10-21",
            "2026-10-22", "2026-10-23", "2026-10-24"
        )
        // Monday's figure sits in the second column, where Monday is in a Sunday-first week.
        assertThat(oldest.days.map { it.views })
            .containsExactly(0L, 0L, 1L, 2L, 3L, 4L, 5L)
        // Oct 18 is the Sunday before anything the API sent, so it is blank, not a quiet day.
        assertThat(oldest.days.first().isInWindow).isFalse()
        assertThat(rows.first().days.map { it.views })
            .containsExactly(6L, 0L, 0L, 0L, 0L, 0L, 0L)
    }

    @Test
    fun `weeks with no days at all are dropped`() {
        val rows = listOf(
            week(MONDAY_OCT_19, views = emptyList()),
            week(MONDAY_OCT_26, views = listOf(1L))
        ).toCalendarRows(DayOfWeek.MONDAY)

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
