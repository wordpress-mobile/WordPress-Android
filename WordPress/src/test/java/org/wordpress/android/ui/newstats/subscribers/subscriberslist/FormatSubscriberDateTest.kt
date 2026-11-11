package org.wordpress.android.ui.newstats.subscribers.subscriberslist

import android.content.res.Resources
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(MockitoJUnitRunner::class)
class FormatSubscriberDateTest {
    @Mock
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        whenever(
            resources.getString(
                R.string.stats_subscriber_since_today
            )
        ).thenReturn("today")

        whenever(
            resources.getQuantityString(
                eq(R.plurals.stats_subscriber_days),
                any(), any()
            )
        ).thenAnswer { invocation ->
            val count = invocation.getArgument<Int>(1)
            if (count == 1) "$count day" else "$count days"
        }

        whenever(
            resources.getQuantityString(
                eq(R.plurals.stats_subscriber_years),
                any(), any()
            )
        ).thenAnswer { invocation ->
            val count = invocation.getArgument<Int>(1)
            if (count == 1) "$count year"
            else "$count years"
        }

        whenever(
            resources.getString(
                eq(
                    R.string
                        .stats_subscriber_years_and_days
                ),
                any(), any()
            )
        ).thenAnswer { invocation ->
            val years = invocation.getArgument<Any>(1)
            val days = invocation.getArgument<Any>(2)
            "$years, $days"
        }
    }

    private fun dateNDaysAgo(n: Long): String =
        LocalDate.now().minusDays(n)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun dateTimeNDaysAgo(n: Long): String =
        LocalDate.now().minusDays(n)
            .atStartOfDay()
            .format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )

    @Test
    fun `when subscribed today, then returns today`() {
        val result = formatSubscriberDate(
            dateNDaysAgo(0), resources
        )
        assertThat(result).isEqualTo("today")
    }

    @Test
    fun `when subscribed 1 day ago, then returns 1 day`() {
        val result = formatSubscriberDate(
            dateNDaysAgo(1), resources
        )
        assertThat(result).isEqualTo("1 day")
    }

    @Test
    fun `when subscribed 30 days ago, then returns 30 days`() {
        val result = formatSubscriberDate(
            dateNDaysAgo(30), resources
        )
        assertThat(result).isEqualTo("30 days")
    }

    @Test
    fun `when subscribed 364 days ago, then returns 364 days`() {
        val result = formatSubscriberDate(
            dateNDaysAgo(364), resources
        )
        assertThat(result).isEqualTo("364 days")
    }

    @Test
    fun `when subscribed exactly 1 year ago, then returns 1 year`() {
        val subscribed = LocalDate.now().minusYears(1)
        val dateStr = subscribed.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        val result = formatSubscriberDate(
            dateStr, resources
        )
        assertThat(result).isEqualTo("1 year")
    }

    @Test
    fun `when subscribed 1 year and 1 day ago, then returns years and days`() {
        val subscribed = LocalDate.now().minusYears(1)
            .minusDays(1)
        val dateStr = subscribed.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        val result = formatSubscriberDate(
            dateStr, resources
        )
        assertThat(result).isEqualTo("1 year, 1 day")
    }

    @Test
    fun `when subscribed 2 years ago, then returns 2 years`() {
        val subscribed = LocalDate.now().minusYears(2)
        val dateStr = subscribed.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        val result = formatSubscriberDate(
            dateStr, resources
        )
        assertThat(result).isEqualTo("2 years")
    }

    @Test
    fun `when subscribed more than 2 years ago, then returns years and days`() {
        val today = LocalDate.now()
        val twoYearsAgo = today.minusYears(2)
        val subscribed = twoYearsAgo.minusDays(50)
        val dateStr = subscribed.format(
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        val period = java.time.Period.between(
            subscribed, today
        )
        val remaining =
            java.time.temporal.ChronoUnit.DAYS.between(
                subscribed.plusYears(
                    period.years.toLong()
                ),
                today
            )
        val result = formatSubscriberDate(
            dateStr, resources
        )
        assertThat(result).isEqualTo(
            "${period.years} years, $remaining days"
        )
    }

    @Test
    fun `when date is ISO datetime format, then parses correctly`() {
        val result = formatSubscriberDate(
            dateTimeNDaysAgo(10), resources
        )
        assertThat(result).isEqualTo("10 days")
    }

    @Test
    fun `when date is ISO date format, then parses correctly`() {
        val result = formatSubscriberDate(
            dateNDaysAgo(10), resources
        )
        assertThat(result).isEqualTo("10 days")
    }

    @Test
    fun `when date string is invalid, then returns original string`() {
        val result = formatSubscriberDate(
            "not-a-date", resources
        )
        assertThat(result).isEqualTo("not-a-date")
    }

    @Test
    fun `when date string is empty, then returns empty string`() {
        val result = formatSubscriberDate("", resources)
        assertThat(result).isEqualTo("")
    }
}
