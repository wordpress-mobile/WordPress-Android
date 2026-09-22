package org.wordpress.android.ui.rs.contentlist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Tests for [ContentDateGrouper], which decides the date bucket each row falls into.
 *
 * "Now" is pinned rather than read from the clock, so the boundaries between buckets can be
 * exercised directly and the results do not drift with the date the suite happens to run on.
 */
class ContentDateGrouperTest {
    private val locale = Locale.US

    /** 2026-09-10 12:00 UTC — mid-month, so "earlier this month" has room on both sides. */
    private val now = utcMillis(year = 2026, month = Calendar.SEPTEMBER, day = 10)

    @Test
    fun `earlier today is still today`() {
        assertThat(groupOf(now - TimeUnit.HOURS.toMillis(6))).isEqualTo(ContentDateGroup.Today)
    }

    @Test
    fun `a day ago is yesterday`() {
        assertThat(groupOf(now - days(1))).isEqualTo(ContentDateGroup.Yesterday)
    }

    @Test
    fun `buckets follow calendar days rather than elapsed hours`() {
        // 23 hours before midday is late *yesterday*, not "today" as a 24-hour window would have
        // it. This is the case that makes a comment posted last night read correctly this morning.
        assertThat(groupOf(now - TimeUnit.HOURS.toMillis(23))).isEqualTo(ContentDateGroup.Yesterday)
    }

    @Test
    fun `two days ago falls through to this week`() {
        assertThat(groupOf(now - days(2))).isEqualTo(ContentDateGroup.ThisWeek)
    }

    @Test
    fun `six days ago is still this week`() {
        assertThat(groupOf(now - days(6))).isEqualTo(ContentDateGroup.ThisWeek)
    }

    @Test
    fun `a future date groups with this week rather than falling through`() {
        assertThat(groupOf(now + days(3))).isEqualTo(ContentDateGroup.ThisWeek)
    }

    @Test
    fun `later the same day is today, not a future bucket`() {
        assertThat(groupOf(now + TimeUnit.HOURS.toMillis(6))).isEqualTo(ContentDateGroup.Today)
    }

    @Test
    fun `eight days ago in the same month is earlier this month`() {
        assertThat(groupOf(now - days(8)))
            .isEqualTo(ContentDateGroup.EarlierThisMonth("September"))
    }

    @Test
    fun `an earlier month in the same year is named without its year`() {
        val july = utcMillis(year = 2026, month = Calendar.JULY, day = 24)

        assertThat(groupOf(july)).isEqualTo(ContentDateGroup.SpecificMonth("July"))
    }

    @Test
    fun `a month in an earlier year carries its year`() {
        val march = utcMillis(year = 2025, month = Calendar.MARCH, day = 2)

        assertThat(groupOf(march)).isEqualTo(ContentDateGroup.SpecificMonth("March 2025"))
    }

    @Test
    fun `the same month in an earlier year is not mistaken for this month`() {
        val lastSeptember = utcMillis(year = 2025, month = Calendar.SEPTEMBER, day = 10)

        assertThat(groupOf(lastSeptember))
            .isEqualTo(ContentDateGroup.SpecificMonth("September 2025"))
    }

    @Test
    fun `each bucket has its own key so headers do not collide`() {
        val keys = listOf(
            ContentDateGroup.Today,
            ContentDateGroup.Yesterday,
            ContentDateGroup.ThisWeek,
            ContentDateGroup.EarlierThisMonth("September"),
            ContentDateGroup.SpecificMonth("September")
        ).map { it.key }

        assertThat(keys).doesNotHaveDuplicates()
    }

    private fun groupOf(millis: Long) = ContentDateGrouper.groupOf(millis, now, locale)

    private fun days(count: Long) = TimeUnit.DAYS.toMillis(count)

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"), locale).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
