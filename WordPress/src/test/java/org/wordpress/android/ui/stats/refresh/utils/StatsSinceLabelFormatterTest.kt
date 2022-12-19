package org.wordpress.android.ui.stats.refresh.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.wordpress.android.R
import org.wordpress.android.viewmodel.ResourceProvider
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class StatsSinceLabelFormatterTest {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.stats_followers_seconds_ago)) } doReturn SECONDS_AGO
        on { getString(eq(R.string.stats_followers_a_minute_ago)) } doReturn ONE_MINUTE_AGO
        on { getString(eq(R.string.stats_followers_minutes), any()) } doAnswer { X_MINUTES_AGO.format(it.arguments[1]) }
        on { getString(eq(R.string.stats_followers_an_hour_ago)) } doReturn ONE_HOUR_AGO
        on { getString(eq(R.string.stats_followers_hours), any()) } doAnswer { X_HOURS_AGO.format(it.arguments[1]) }
        on { getString(eq(R.string.stats_followers_a_day)) } doReturn ONE_DAY_AGO
        on { getString(eq(R.string.stats_followers_days), any()) } doAnswer { X_DAYS_AGO.format(it.arguments[1]) }
        on { getString(eq(R.string.stats_followers_a_month)) } doReturn ONE_MONTH_AGO
        on { getString(eq(R.string.stats_followers_months), any()) } doAnswer { X_MONTHS_AGO.format(it.arguments[1]) }
        on { getString(eq(R.string.stats_followers_a_year)) } doReturn ONE_YEAR_AGO
        on { getString(eq(R.string.stats_followers_years), any()) } doAnswer { X_YEARS_AGO.format(it.arguments[1]) }
    }

    private val formatter = StatsSinceLabelFormatter(resourceProvider)

    @Test
    fun `get since seconds label`() {
        makeAssertions(
                closestValidDate = now,
                farthestValidDate = fortyFiveSecondsAgo.plusSeconds(1),
                expectedLabel = SECONDS_AGO
        )
    }

    @Test
    fun `get since a minute label`() {
        makeAssertions(
                closestValidDate = fortyFiveSecondsAgo,
                farthestValidDate = ninetySecondsAgo.plusSeconds(1),
                expectedLabel = ONE_MINUTE_AGO
        )
    }

    @Test
    fun `get since minutes label`() {
        makeAssertions(
                closestValidDate = ninetySecondsAgo,
                farthestValidDate = fortyFiveMinutesAgo.plusSeconds(1),
                expectedLabelOnClosestDate = X_MINUTES_AGO.format(2),
                expectedLabelOnFarthestDate = X_MINUTES_AGO.format(45)
        )
    }

    @Test
    fun `get since an hour label`() {
        makeAssertions(
                closestValidDate = fortyFiveMinutesAgo,
                farthestValidDate = ninetyMinutesAgo.plusSeconds(1),
                expectedLabel = ONE_HOUR_AGO
        )
    }

    @Test
    fun `get since hours label`() {
        makeAssertions(
                closestValidDate = ninetyMinutesAgo,
                farthestValidDate = twentyTwoHoursAgo.plusSeconds(1),
                expectedLabelOnClosestDate = X_HOURS_AGO.format(2),
                expectedLabelOnFarthestDate = X_HOURS_AGO.format(22)
        )
    }

    @Test
    fun `get since a day label`() {
        makeAssertions(
                closestValidDate = twentyTwoHoursAgo,
                farthestValidDate = thirtySixHoursAgo.plusSeconds(1),
                expectedLabel = ONE_DAY_AGO
        )
    }

    @Test
    fun `get since days label`() {
        makeAssertions(
                closestValidDate = thirtySixHoursAgo,
                farthestValidDate = twentyFiveDaysAgo.plusSeconds(1),
                expectedLabelOnClosestDate = X_DAYS_AGO.format(2),
                expectedLabelOnFarthestDate = X_DAYS_AGO.format(25)
        )
    }

    @Test
    fun `get since a month label`() {
        makeAssertions(
                closestValidDate = twentyFiveDaysAgo,
                farthestValidDate = fortyFiveDaysAgo.plusSeconds(1),
                expectedLabel = ONE_MONTH_AGO
        )
    }

    @Test
    fun `get since months label`() {
        makeAssertions(
                closestValidDate = fortyFiveDaysAgo,
                farthestValidDate = threeHundredAndTwentyDaysAgo.plusSeconds(1),
                expectedLabelOnClosestDate = X_MONTHS_AGO.format(2),
                expectedLabelOnFarthestDate = X_MONTHS_AGO.format(11)
        )
    }

    @Test
    fun `get since a year label`() {
        makeAssertions(
                closestValidDate = threeHundredAndTwentyDaysAgo,
                farthestValidDate = fiveHundredAndFortyEightDaysAgo.plusSeconds(1),
                expectedLabel = ONE_YEAR_AGO
        )
    }

    @Test
    fun `get since years label`() {
        makeAssertions(
                closestValidDate = fiveHundredAndFortyEightDaysAgo,
                farthestValidDate = now.minusYears(20).plusSeconds(1),
                expectedLabelOnClosestDate = X_YEARS_AGO.format(2),
                expectedLabelOnFarthestDate = X_YEARS_AGO.format(20)
        )
    }

    private fun makeAssertions(
        closestValidDate: ZonedDateTime,
        farthestValidDate: ZonedDateTime,
        expectedLabel: String
    ) {
        makeAssertions(closestValidDate, farthestValidDate, expectedLabel, expectedLabel)
    }

    private fun makeAssertions(
        closestValidDate: ZonedDateTime,
        farthestValidDate: ZonedDateTime,
        expectedLabelOnClosestDate: String,
        expectedLabelOnFarthestDate: String
    ) {
        assertThat(formatter.getSinceLabel(closestValidDate, now)).isEqualTo(expectedLabelOnClosestDate)
        assertThat(formatter.getSinceLabel(farthestValidDate, now)).isEqualTo(expectedLabelOnFarthestDate)
    }

    companion object {
        private val now = LocalDateTime.of(2022, 4, 12, 20, 25, 20).atZone(ZoneOffset.UTC)
        private val fortyFiveSecondsAgo = now.minusSeconds(45)
        private val ninetySecondsAgo = now.minusSeconds(90)
        private val fortyFiveMinutesAgo = now.minusMinutes(45)
        private val ninetyMinutesAgo = now.minusMinutes(90)
        private val twentyTwoHoursAgo = now.minusHours(22)
        private val thirtySixHoursAgo = now.minusHours(36)
        private val twentyFiveDaysAgo = now.minusDays(25)
        private val fortyFiveDaysAgo = now.minusDays(45)
        private val threeHundredAndTwentyDaysAgo = now.minusDays(320)
        private val fiveHundredAndFortyEightDaysAgo = now.minusDays(548)

        const val SECONDS_AGO = "seconds ago"
        const val ONE_MINUTE_AGO = "a minute ago"
        const val X_MINUTES_AGO = "%d minutes"
        const val ONE_HOUR_AGO = "an hour ago"
        const val X_HOURS_AGO = "%d hours"
        const val ONE_DAY_AGO = "a day"
        const val X_DAYS_AGO = "%d days"
        const val ONE_MONTH_AGO = "a month"
        const val X_MONTHS_AGO = "%d months"
        const val ONE_YEAR_AGO = "a year"
        const val X_YEARS_AGO = "%d years"
    }
}
