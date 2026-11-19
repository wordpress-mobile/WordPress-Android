package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.newstats.ChartInterval
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.datasource.CommentsDataPoint
import org.wordpress.android.ui.newstats.datasource.LikesDataPoint
import org.wordpress.android.ui.newstats.datasource.PostsDataPoint
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import org.wordpress.android.ui.newstats.datasource.VisitorsDataPoint
import org.wordpress.android.ui.newstats.datasource.VisitsDataPoint
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The chart interval re-slices the windows a period resolves to; it must never move them.
 *
 * "Today" is pinned to Thursday 19 November 2026 throughout, and the locale to US, so a week's
 * first day — which the calendar-aligned windows use and the API's Monday-anchored week buckets
 * deliberately don't — stays fixed.
 */
@ExperimentalCoroutinesApi
class StatsRepositoryChartIntervalTest : BaseUnitTest() {
    @Mock
    private lateinit var statsDataSource: StatsDataSource

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var repository: StatsRepository

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        repository = StatsRepository(
            statsDataSource = statsDataSource,
            statsResultCache = StatsResultCache(),
            appLogWrapper = appLogWrapper,
            clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()),
            ioDispatcher = testDispatcher()
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    // region Automatic changes nothing
    @Test
    fun `given Automatic, when a rolling preset is charted, then its own unit and bucket count are sent`() = test {
        // The rolling path now measures each window instead of reusing the preset's declared
        // quantity. For every preset on its own canonical window the two agree by construction, and
        // this is what pins that: a mismatch would silently reshape a chart nobody touched.
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last7Days)
        verifyRequest(unit = StatsUnit.DAY, quantity = 7, endDate = "2026-11-19", startDate = null)
        verifyRequest(unit = StatsUnit.DAY, quantity = 7, endDate = "2026-11-12", startDate = null)

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last30Days)
        verifyRequest(unit = StatsUnit.DAY, quantity = 30, endDate = "2026-11-19", startDate = null)
        verifyRequest(unit = StatsUnit.DAY, quantity = 30, endDate = "2026-10-20", startDate = null)

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last12Months)
        verifyRequest(unit = StatsUnit.MONTH, quantity = 12, endDate = "2026-11-19", startDate = "2025-12-01")
        verifyRequest(unit = StatsUnit.MONTH, quantity = 12, endDate = "2025-11-19", startDate = "2024-12-01")

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last3Years)
        verifyRequest(unit = StatsUnit.YEAR, quantity = 3, endDate = "2026-11-19", startDate = "2024-01-01")
        verifyRequest(unit = StatsUnit.YEAR, quantity = 3, endDate = "2023-11-19", startDate = "2021-01-01")
    }

    @Test
    fun `given Automatic, when a single day is charted, then it is still hourly`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Today)
        verifyRequest(unit = StatsUnit.HOUR, quantity = 24, endDate = "2026-11-19 23:00:00", startDate = null)
        verifyRequest(unit = StatsUnit.HOUR, quantity = 24, endDate = "2026-11-18 23:00:00", startDate = null)

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-11-10", "2026-11-10"))
        verifyRequest(unit = StatsUnit.HOUR, quantity = 24, endDate = "2026-11-10 23:00:00", startDate = null)
        verifyRequest(unit = StatsUnit.HOUR, quantity = 24, endDate = "2026-11-09 23:00:00", startDate = null)
    }

    @Test
    fun `given Automatic, when a multi-day custom range is charted, then nothing about it changes`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-02-11", "2026-10-07"))

        verifyRequest(unit = StatsUnit.MONTH, quantity = 9, endDate = "2026-10-07", startDate = "2026-02-11")
        verifyRequest(unit = StatsUnit.MONTH, quantity = 9, endDate = "2026-02-10", startDate = "2025-06-17")
    }
    // endregion

    // region An override re-slices the window it is given
    @Test
    fun `given Months on Last 7 Days, then the window is still seven days, truncated to them`() = test {
        // The case the whole design turns on. Without its own start_date the API would answer with
        // the whole of November, and every number on the card would describe a month the user did
        // not pick.
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last7Days, ChartInterval.MONTHS)

        verifyRequest(unit = StatsUnit.MONTH, quantity = 1, endDate = "2026-11-19", startDate = "2026-11-13")
        verifyRequest(unit = StatsUnit.MONTH, quantity = 1, endDate = "2026-11-12", startDate = "2026-11-06")
    }

    @Test
    fun `given Months on Today, then the day is charted as one month bucket`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Today, ChartInterval.MONTHS)

        // No "23:00:00": only an hourly window ends at an exact hour.
        verifyRequest(unit = StatsUnit.MONTH, quantity = 1, endDate = "2026-11-19", startDate = "2026-11-19")
        verifyRequest(unit = StatsUnit.MONTH, quantity = 1, endDate = "2026-11-18", startDate = "2026-11-18")
    }

    @Test
    fun `given Days on a single-day custom range, then the hourly short-circuit is bypassed`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-11-10", "2026-11-10"), ChartInterval.DAYS)

        verifyRequest(unit = StatsUnit.DAY, quantity = 1, endDate = "2026-11-10", startDate = null)
        verifyRequest(unit = StatsUnit.DAY, quantity = 1, endDate = "2026-11-09", startDate = null)
    }

    @Test
    fun `given Weeks on a six-week custom range, then both windows are counted in week buckets`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-10-05", "2026-11-15"), ChartInterval.WEEKS)

        verifyRequest(unit = StatsUnit.WEEK, quantity = 6, endDate = "2026-11-15", startDate = "2026-10-05")
        verifyRequest(unit = StatsUnit.WEEK, quantity = 6, endDate = "2026-10-04", startDate = "2026-08-24")
    }

    @Test
    fun `given Weeks, when a window straddles two Mondays, then it spans two buckets`() = test {
        // A week bucket is anchored to its Monday whatever the locale, so a Friday-to-Tuesday window
        // is less than a week long and still two bars.
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-11-13", "2026-11-17"), ChartInterval.WEEKS)

        verifyRequest(unit = StatsUnit.WEEK, quantity = 2, endDate = "2026-11-17", startDate = "2026-11-13")
    }

    @Test
    fun `given Weeks, when a window is one Monday-to-Sunday week, then it spans one bucket`() = test {
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, customPeriod("2026-11-09", "2026-11-15"), ChartInterval.WEEKS)

        verifyRequest(unit = StatsUnit.WEEK, quantity = 1, endDate = "2026-11-15", startDate = "2026-11-09")
    }

    @Test
    fun `given Weeks on This Month, then the elapsed and whole-previous windows are both re-sliced`() = test {
        // The calendar path keeps comparing this month so far against all of last month; only the
        // bucket width changes.
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.ThisMonth, ChartInterval.WEEKS)

        verifyRequest(unit = StatsUnit.WEEK, quantity = 4, endDate = "2026-11-19", startDate = "2026-11-01")
        verifyRequest(unit = StatsUnit.WEEK, quantity = 5, endDate = "2026-10-31", startDate = "2026-10-01")
    }

    @Test
    fun `given an override, then the previous window stays where it was`() = test {
        // The comparison is re-sliced, not moved: Last 12 Months still reads against the twelve
        // months before it, which is what keeps the header's percentage like-for-like.
        stubSuccess()

        repository.fetchStatsForPeriod(TEST_SITE_ID, StatsPeriod.Last12Months, ChartInterval.WEEKS)

        // The two windows cover the same twelve months but a different number of week buckets, since
        // neither starts on a Monday: each is measured on its own span rather than sharing a count.
        verifyRequest(unit = StatsUnit.WEEK, quantity = 51, endDate = "2026-11-19", startDate = "2025-12-01")
        verifyRequest(unit = StatsUnit.WEEK, quantity = 52, endDate = "2025-11-19", startDate = "2024-12-01")
    }
    // endregion

    // region chartUnitFor
    @Test
    fun `chartUnitFor reports the derived unit under Automatic and the chosen one otherwise`() {
        assertThat(repository.chartUnitFor(StatsPeriod.Today, ChartInterval.AUTOMATIC)).isEqualTo(StatsUnit.HOUR)
        assertThat(repository.chartUnitFor(StatsPeriod.Last7Days, ChartInterval.AUTOMATIC)).isEqualTo(StatsUnit.DAY)
        assertThat(repository.chartUnitFor(StatsPeriod.Last12Months, ChartInterval.AUTOMATIC))
            .isEqualTo(StatsUnit.MONTH)
        assertThat(repository.chartUnitFor(StatsPeriod.Last3Years, ChartInterval.AUTOMATIC)).isEqualTo(StatsUnit.YEAR)
        assertThat(repository.chartUnitFor(StatsPeriod.ThisYear, ChartInterval.AUTOMATIC)).isEqualTo(StatsUnit.MONTH)

        assertThat(repository.chartUnitFor(StatsPeriod.Today, ChartInterval.DAYS)).isEqualTo(StatsUnit.DAY)
        assertThat(repository.chartUnitFor(StatsPeriod.Last7Days, ChartInterval.WEEKS)).isEqualTo(StatsUnit.WEEK)
        assertThat(repository.chartUnitFor(customPeriod("2026-11-10", "2026-11-10"), ChartInterval.DAYS))
            .isEqualTo(StatsUnit.DAY)
    }
    // endregion

    // region offeredIntervals
    @Test
    fun `offeredIntervals covers each window's band`() {
        assertThat(repository.offeredIntervals(StatsPeriod.Today))
            .containsExactly(ChartInterval.AUTOMATIC, ChartInterval.HOURS)
        assertThat(repository.offeredIntervals(StatsPeriod.Last7Days))
            .containsExactly(ChartInterval.AUTOMATIC, ChartInterval.HOURS, ChartInterval.DAYS, ChartInterval.WEEKS)
        assertThat(repository.offeredIntervals(StatsPeriod.Last30Days))
            .containsExactly(ChartInterval.AUTOMATIC, ChartInterval.DAYS, ChartInterval.WEEKS, ChartInterval.MONTHS)
        assertThat(repository.offeredIntervals(StatsPeriod.Last3Years))
            .containsExactly(ChartInterval.AUTOMATIC, ChartInterval.WEEKS, ChartInterval.MONTHS, ChartInterval.YEARS)
    }

    @Test
    fun `offeredIntervals always contains the unit Automatic resolves to`() {
        val periods = listOf(
            StatsPeriod.Today,
            StatsPeriod.Last7Days,
            StatsPeriod.Last30Days,
            StatsPeriod.Last12Months,
            StatsPeriod.Last3Years,
            StatsPeriod.ThisWeek,
            StatsPeriod.ThisMonth,
            StatsPeriod.ThisYear,
            customPeriod("2026-11-10", "2026-11-10"),
            customPeriod("2026-10-05", "2026-11-15"),
            customPeriod("2020-01-01", "2026-11-15")
        )

        periods.forEach { period ->
            val derived = repository.chartUnitFor(period, ChartInterval.AUTOMATIC)
            assertThat(repository.offeredIntervals(period).mapNotNull { it.unit })
                .describedAs("offered units for %s", period)
                .contains(derived)
        }
    }

    @Test
    fun `offeredIntervals measures the whole calendar period, not the part of it that has elapsed`() {
        // This Week is five days old on a Thursday but charts the whole Monday-to-Sunday span, so the
        // menu must not gain and lose entries as the week fills up.
        assertThat(repository.offeredIntervals(StatsPeriod.ThisWeek))
            .containsExactly(ChartInterval.AUTOMATIC, ChartInterval.HOURS, ChartInterval.DAYS, ChartInterval.WEEKS)
    }

    @Test
    fun `offeredIntervals drops a unit that would draw a single bar or several hundred`() {
        // One bar is not a chart: a day is one day, one week and one month alike.
        assertThat(repository.offeredIntervals(StatsPeriod.Today))
            .doesNotContain(ChartInterval.DAYS, ChartInterval.WEEKS, ChartInterval.MONTHS, ChartInterval.YEARS)
        // 16 days is 384 hourly bars; 17 is 408.
        assertThat(repository.offeredIntervals(customPeriod("2026-11-01", "2026-11-16")))
            .contains(ChartInterval.HOURS)
        assertThat(repository.offeredIntervals(customPeriod("2026-11-01", "2026-11-17")))
            .doesNotContain(ChartInterval.HOURS)
    }
    // endregion

    private fun customPeriod(start: String, end: String) =
        StatsPeriod.Custom(LocalDate.parse(start), LocalDate.parse(end))

    private suspend fun stubSuccess() {
        whenever(statsDataSource.fetchStatsVisits(any(), any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(StatsVisitsDataResult.Success(statsVisitsData()))
    }

    private suspend fun verifyRequest(unit: StatsUnit, quantity: Int, endDate: String, startDate: String?) {
        verify(statsDataSource).fetchStatsVisits(
            siteId = eq(TEST_SITE_ID),
            unit = eq(unit),
            quantity = eq(quantity),
            endDate = eq(endDate),
            startDate = if (startDate == null) isNull() else eq(startDate),
            statFields = anyOrNull()
        )
    }

    private fun statsVisitsData() = StatsVisitsData(
        visits = listOf(VisitsDataPoint("2026-11-19", 10L)),
        visitors = listOf(VisitorsDataPoint("2026-11-19", 5L)),
        likes = listOf(LikesDataPoint("2026-11-19", 1L)),
        comments = listOf(CommentsDataPoint("2026-11-19", 1L)),
        posts = listOf(PostsDataPoint("2026-11-19", 1L))
    )

    companion object {
        private const val TEST_SITE_ID = 12345L
        private val TODAY = LocalDate.of(2026, 11, 19)
    }
}
