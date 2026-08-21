package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.datasource.CityViewsDataResult
import org.wordpress.android.ui.newstats.datasource.ClicksDataResult
import org.wordpress.android.ui.newstats.datasource.CountryViewsDataResult
import org.wordpress.android.ui.newstats.datasource.DevicesDataResult
import org.wordpress.android.ui.newstats.datasource.FileDownloadsDataResult
import org.wordpress.android.ui.newstats.datasource.ReferrersDataResult
import org.wordpress.android.ui.newstats.datasource.RegionViewsDataResult
import org.wordpress.android.ui.newstats.datasource.SearchTermsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.StatsInsightsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsTagsData
import org.wordpress.android.ui.newstats.datasource.StatsTagsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsSummaryDataResult
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.datasource.StatsDateRange
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.datasource.StatsVisitField
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import org.wordpress.android.ui.newstats.datasource.TopAuthorsDataResult
import org.wordpress.android.ui.newstats.datasource.TopPostsDataResult
import org.wordpress.android.ui.newstats.datasource.VideoPlaysDataResult
import org.wordpress.android.ui.newstats.datasource.StatsSubscribersDataResult
import org.wordpress.android.ui.newstats.datasource.SubscribersByUserTypeDataResult
import org.wordpress.android.ui.newstats.datasource.StatsEmailsSummaryDataResult
import org.wordpress.android.ui.newstats.datasource.UtmDataResult
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.StatsPeriod
import androidx.annotation.StringRes
import org.wordpress.android.ui.newstats.datasource.StatsErrorType
import org.wordpress.android.util.AppLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException

private const val HOURLY_QUANTITY = 24
// Hourly queries end at an exact hour. "<day> 23:00:00" makes the 24-hour window cover that calendar
// day's 00:00–23:00 buckets; a date-only value (00:00) shifts the window back an hour and drops the
// day's 00:00 bucket.
private const val HOURLY_END_TIME = "23:00:00"
private const val DAILY_QUANTITY = 1
private const val WEEKLY_QUANTITY = 7
private const val DAYS_BEFORE_END_DATE = -6
private const val DAYS_IN_7_DAYS = 7
private const val DAYS_IN_30_DAYS = 30
private const val DAYS_IN_6_MONTHS = 182
private const val DAYS_IN_12_MONTHS = 365
private const val MONTHS_IN_6_MONTHS = 6
private const val MONTHS_IN_12_MONTHS = 12
private const val MAX_DAYS_IN_MONTH = 31
private const val MAX_DAYS_IN_2_YEARS = 731
// Backward paging stops once the window's start would reach this year, matching iOS's floor.
private const val NAVIGATION_FLOOR_YEAR = 2000
// The metrics the Views card needs from a stats/visits call: `views` drives the chart line and the
// header total; the rest fill the bottom-row totals. For non-hourly periods a single [fetchStatsForPeriod]
// call returns all of them, so the card needs no separate bottom-row request. Hourly (single-day)
// responses only populate `views`, so those periods fetch the bottom row from a dedicated day-level call.
private val CARD_STAT_FIELDS = listOf(
    StatsVisitField.VIEWS,
    StatsVisitField.VISITORS,
    StatsVisitField.LIKES,
    StatsVisitField.COMMENTS,
    StatsVisitField.POSTS
)
private const val PERCENTAGE_MULTIPLIER = 100.0
private const val PERCENTAGE_NO_CHANGE = 0.0

/**
 * Calculates the percentage change between a current and previous value.
 * Returns [PERCENTAGE_MULTIPLIER] (100%) when previous is 0 but current > 0,
 * and [PERCENTAGE_NO_CHANGE] (0%) when both are 0.
 */
internal fun calculateItemChangePercent(
    current: Long,
    previous: Long
): Double {
    val change = current - previous
    return if (previous > 0) {
        (change.toDouble() / previous.toDouble()) *
            PERCENTAGE_MULTIPLIER
    } else if (current > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
}
private const val NUM_DAYS_TODAY = 1
private const val SUBSCRIBERS_DEFAULT_MAX = 10

// Number of referrers requested for the card. The detail screen requests all of them (max = 0,
// which the server treats as "unlimited").
private const val REFERRERS_CARD_MAX = 10
private const val REFERRERS_DETAIL_MAX = 0

/**
 * Repository for fetching stats data using the wordpress-rs API.
 * Handles hourly visits/views data for the Today's Stats card chart.
 */
@Suppress("LargeClass")
class StatsRepository @Inject constructor(
    private val statsDataSource: StatsDataSource,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    fun init(accessToken: String) {
        statsDataSource.init(accessToken)
    }

    /**
     * The concrete range immediately before [period], stepped by the period's own length.
     *
     * The step is the window measured in its natural bucket unit ([unitAndQuantityFor]): Today steps
     * one day, Last 7 Days seven days, Last 6 Months six months. Both endpoints shift by the same
     * calendar amount so the span is preserved, and the result is always a concrete
     * [StatsPeriod.Custom] — navigating away from a preset drops the preset (matching iOS), and a
     * single-day result renders hourly exactly as Today does.
     */
    fun previousPeriod(period: StatsPeriod): StatsPeriod = shiftPeriod(period, direction = -1)

    /**
     * The range immediately after [period], stepped by the period's own length. Clamped so the window
     * never ends after today; callers gate the control with [canNavigateForward]. When the result
     * lands exactly on a preset's own window it is returned as that preset (see [snapToPreset]), so
     * paging forward onto the present edge restores the original label — Today → back → forward shows
     * "Today", not today's date.
     */
    fun nextPeriod(period: StatsPeriod): StatsPeriod {
        val shifted = shiftPeriod(period, direction = 1)
        // Windows generated by navigation always end on a step boundary from today, so forward lands
        // exactly on the present edge. Clamp defensively in case a manually-picked range that isn't
        // aligned to today overshoots.
        val today = LocalDate.now()
        val result = if (shifted.endDate > today) {
            val span = ChronoUnit.DAYS.between(shifted.startDate, shifted.endDate)
            StatsPeriod.Custom(today.minusDays(span), today)
        } else {
            shifted
        }
        return snapToPreset(result)
    }

    /**
     * Returns the preset whose canonical window exactly equals [range], or [range] itself when none
     * matches. Lets forward navigation that lands back on the present edge restore the preset label
     * instead of an equivalent Custom range. Only forward can match — every preset window ends today.
     * Day-based presets match exactly; a month preset whose window drifted by a short-month clamp
     * stays Custom, a safe fallback (identical data, shown as dates rather than the preset name).
     */
    private fun snapToPreset(range: StatsPeriod.Custom): StatsPeriod {
        val window = range.startDate to range.endDate
        return StatsPeriod.presets().firstOrNull { currentPeriodWindow(it) == window } ?: range
    }

    /** Whether paging backward stays above the year-[NAVIGATION_FLOOR_YEAR] floor. */
    fun canNavigateBackward(period: StatsPeriod): Boolean {
        val (start, _) = currentPeriodWindow(period)
        return start.year > NAVIGATION_FLOOR_YEAR
    }

    /** Whether paging forward stays at or before today (the present edge). */
    fun canNavigateForward(period: StatsPeriod): Boolean {
        val (_, end) = currentPeriodWindow(period)
        return end < LocalDate.now()
    }

    /**
     * Shifts [period]'s concrete window by its own length in [direction] (-1 = back, +1 = forward),
     * returning a concrete [StatsPeriod.Custom]. The shift amount is `quantity × direction` in the
     * window's own bucket unit, so the span is preserved and month/year windows step by whole
     * calendar months/years rather than by a fixed number of days.
     */
    private fun shiftPeriod(period: StatsPeriod, direction: Int): StatsPeriod.Custom {
        val (start, end) = currentPeriodWindow(period)
        val (unit, quantity) = unitAndQuantityFor(start, end)
        val amount = quantity.toLong() * direction
        val (newStart, newEnd) = when (unit) {
            StatsUnit.YEAR -> start.plusYears(amount) to end.plusYears(amount)
            StatsUnit.MONTH -> start.plusMonths(amount) to end.plusMonths(amount)
            else -> start.plusDays(amount) to end.plusDays(amount)
        }
        return StatsPeriod.Custom(newStart, newEnd)
    }

    /**
     * Fetches today's aggregated stats (views, visitors, likes, comments).
     *
     * @param siteId The WordPress.com site ID
     * @return Today's aggregated stats or error
     */
    suspend fun fetchTodayAggregates(siteId: Long): TodayAggregatesResult = withContext(ioDispatcher) {
        val dateString = LocalDate.now().format(dateFormatter)

        val result = statsDataSource.fetchStatsVisits(
            siteId = siteId,
            unit = StatsUnit.DAY,
            quantity = DAILY_QUANTITY,
            endDate = dateString
        )

        when (result) {
            is StatsVisitsDataResult.Success -> {
                val data = result.data
                val views = data.visits.firstOrNull()?.visits ?: 0L
                val visitors = data.visitors.firstOrNull()?.visitors ?: 0L
                val likes = data.likes.firstOrNull()?.likes ?: 0L
                val comments = data.comments.firstOrNull()?.comments ?: 0L

                val aggregates = TodayAggregates(
                    views = views,
                    visitors = visitors,
                    likes = likes,
                    comments = comments
                )
                TodayAggregatesResult.Success(aggregates)
            }

            is StatsVisitsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "API Error fetching today aggregates: ${result.errorType}"
                )
                TodayAggregatesResult.Error(result.errorType.name)
            }
        }
    }

    /**
     * Fetches hourly views data for the specified date.
     *
     * The window ends at the target day itself ([formatApiEndDate] appends "23:00:00"), so its 24
     * buckets cover that calendar day's 00:00–23:00. Passing the next day's date instead would end the
     * window at 00:00, shifting it back an hour: the day's own 00:00 bucket would be dropped in favour
     * of the following day's.
     *
     * @param siteId The WordPress.com site ID
     * @param offsetDays Number of days to offset from today (0 = today, 1 = yesterday, etc.)
     * @return List of hourly views data points, or empty list if fetch fails
     */
    suspend fun fetchHourlyViews(
        siteId: Long,
        offsetDays: Int = 0
    ): HourlyViewsResult = withContext(ioDispatcher) {
        val day = LocalDate.now().minusDays(offsetDays.toLong())

        val result = statsDataSource.fetchStatsVisits(
            siteId = siteId,
            unit = StatsUnit.HOUR,
            quantity = HOURLY_QUANTITY,
            endDate = formatApiEndDate(day, StatsUnit.HOUR)
        )

        when (result) {
            is StatsVisitsDataResult.Success -> {
                val dataPoints = result.data.visits.map { dataPoint ->
                    HourlyViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
                }
                HourlyViewsResult.Success(dataPoints)
            }

            is StatsVisitsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "API Error fetching hourly views: ${result.errorType}"
                )
                HourlyViewsResult.Error(result.errorType.name)
            }
        }
    }

    /**
     * Fetches aggregated weekly stats (views, visitors, likes, comments, posts).
     *
     * @param siteId The WordPress.com site ID
     * @param weeksAgo Number of weeks to go back (0 = current week ending today, 1 = previous week)
     * @return Weekly aggregated stats or error
     */
    suspend fun fetchWeeklyStats(siteId: Long, weeksAgo: Int = 0): WeeklyStatsResult =
        withContext(ioDispatcher) {
            val (startDate, endDate) = calculateWeekDateRange(weeksAgo)
            val endDateString = endDate.format(dateFormatter)

            val result = statsDataSource.fetchStatsVisits(
                siteId = siteId,
                unit = StatsUnit.DAY,
                quantity = WEEKLY_QUANTITY,
                endDate = endDateString
            )

            when (result) {
                is StatsVisitsDataResult.Success -> {
                    val data = result.data
                    val totalViews = data.visits.sumOf { it.visits }
                    val totalVisitors = data.visitors.sumOf { it.visitors }
                    val totalLikes = data.likes.sumOf { it.likes }
                    val totalComments = data.comments.sumOf { it.comments }
                    val totalPosts = data.posts.sumOf { it.posts }

                    val startDateFormatted = startDate.format(dateFormatter)

                    val aggregates = PeriodAggregates(
                        views = totalViews,
                        visitors = totalVisitors,
                        likes = totalLikes,
                        comments = totalComments,
                        posts = totalPosts,
                        startDate = startDateFormatted,
                        endDate = endDateString
                    )
                    WeeklyStatsResult.Success(aggregates)
                }

                is StatsVisitsDataResult.Error -> {
                    appLogWrapper.e(
                        AppLog.T.STATS,
                        "API Error fetching weekly stats: ${result.errorType}"
                    )
                    WeeklyStatsResult.Error(result.errorType.name)
                }
            }
        }

    /**
     * Fetches daily views data for a specific week.
     *
     * @param siteId The WordPress.com site ID
     * @param weeksAgo Number of weeks to go back (0 = current week ending today, 1 = previous week)
     * @return List of daily views data points or error
     */
    suspend fun fetchDailyViewsForWeek(siteId: Long, weeksAgo: Int = 0): DailyViewsResult =
        withContext(ioDispatcher) {
            val (_, endDate) = calculateWeekDateRange(weeksAgo)
            val endDateString = endDate.format(dateFormatter)

            val result = statsDataSource.fetchStatsVisits(
                siteId = siteId,
                unit = StatsUnit.DAY,
                quantity = WEEKLY_QUANTITY,
                endDate = endDateString
            )

            when (result) {
                is StatsVisitsDataResult.Success -> {
                    val dataPoints = result.data.toMetricDataPoints()
                    DailyViewsResult.Success(dataPoints)
                }

                is StatsVisitsDataResult.Error -> {
                    appLogWrapper.e(
                        AppLog.T.STATS,
                        "API Error fetching daily views: ${result.errorType}"
                    )
                    DailyViewsResult.Error(result.errorType.name)
                }
            }
        }

    /**
     * Fetches both aggregated weekly stats AND daily data points in a single API call.
     * This is more efficient than calling fetchWeeklyStats and fetchDailyViewsForWeek separately.
     *
     * @param siteId The WordPress.com site ID
     * @param weeksAgo Number of weeks to go back (0 = current week ending today, 1 = previous week)
     * @return Combined weekly stats with daily data points or error
     */
    suspend fun fetchWeeklyStatsWithDailyData(
        siteId: Long,
        weeksAgo: Int = 0
    ): WeeklyStatsWithDailyDataResult = withContext(ioDispatcher) {
        val (startDate, endDate) = calculateWeekDateRange(weeksAgo)
        val endDateString = endDate.format(dateFormatter)

        val result = statsDataSource.fetchStatsVisits(
            siteId = siteId,
            unit = StatsUnit.DAY,
            quantity = WEEKLY_QUANTITY,
            endDate = endDateString
        )

        when (result) {
            is StatsVisitsDataResult.Success -> {
                val data = result.data

                // Build aggregates
                val totalViews = data.visits.sumOf { it.visits }
                val totalVisitors = data.visitors.sumOf { it.visitors }
                val totalLikes = data.likes.sumOf { it.likes }
                val totalComments = data.comments.sumOf { it.comments }
                val totalPosts = data.posts.sumOf { it.posts }
                val startDateFormatted = startDate.format(dateFormatter)

                val aggregates = PeriodAggregates(
                    views = totalViews,
                    visitors = totalVisitors,
                    likes = totalLikes,
                    comments = totalComments,
                    posts = totalPosts,
                    startDate = startDateFormatted,
                    endDate = endDateString
                )

                // Build daily data points
                val dailyDataPoints = data.toMetricDataPoints()

                WeeklyStatsWithDailyDataResult.Success(aggregates, dailyDataPoints)
            }

            is StatsVisitsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "API Error fetching weekly stats with daily data: " +
                        "${result.errorType}"
                )
                WeeklyStatsWithDailyDataResult.Error(result.errorType.name)
            }
        }
    }

    /**
     * Fetches the chart data for a specific period with comparison to the previous period.
     *
     * Only the two chart windows (current + previous) are fetched here. The bottom-row totals load
     * separately via [fetchBottomStats] so the chart can render as soon as its own calls return,
     * independently of the bottom row (a slow or failed bottom call never blocks the chart).
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Chart stats for current and previous periods or error
     */
    suspend fun fetchStatsForPeriod(
        siteId: Long,
        period: StatsPeriod
    ): PeriodStatsResult = withContext(ioDispatcher) {
        val periodRange = calculatePeriodDates(period)

        val currentEndString = formatApiEndDate(periodRange.currentEnd, periodRange.unit)
        val previousEndString = formatApiEndDate(periodRange.previousEnd, periodRange.unit)

        // Fetch the current and previous chart periods in parallel. Both request [CARD_STAT_FIELDS] so
        // a single call powers the chart (views) and, for non-hourly periods, the bottom-row totals
        // too — letting the card avoid a separate bottom-row request. Hourly responses only populate
        // `views`; those (single-day) periods source the bottom row from a dedicated day-level call.
        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async {
                statsDataSource.fetchStatsVisits(
                    siteId = siteId,
                    unit = periodRange.unit,
                    quantity = periodRange.currentQuantity,
                    endDate = currentEndString,
                    startDate = apiStartDateOrNull(periodRange.currentStart, periodRange.unit),
                    statFields = CARD_STAT_FIELDS
                )
            }
            val previousDeferred = async {
                statsDataSource.fetchStatsVisits(
                    siteId = siteId,
                    unit = periodRange.unit,
                    quantity = periodRange.previousQuantity,
                    endDate = previousEndString,
                    startDate = apiStartDateOrNull(periodRange.previousStart, periodRange.unit),
                    statFields = CARD_STAT_FIELDS
                )
            }
            currentDeferred.await() to previousDeferred.await()
        }

        if (currentResult is StatsVisitsDataResult.Success &&
            previousResult is StatsVisitsDataResult.Success
        ) {
            buildPeriodStatsSuccess(currentResult.data, previousResult.data, periodRange)
        } else {
            buildPeriodStatsError(currentResult, previousResult)
        }
    }

    /**
     * Formats the API `date` (window end) for [date] at [unit]. Hourly windows end at an exact hour
     * ("<day> 23:00:00") so the 24 buckets cover that calendar day's 00:00–23:00; all coarser units use
     * the plain date. See [HOURLY_END_TIME].
     */
    private fun formatApiEndDate(date: LocalDate, unit: StatsUnit): String {
        val dateString = date.format(dateFormatter)
        return if (unit == StatsUnit.HOUR) "$dateString $HOURLY_END_TIME" else dateString
    }

    /**
     * The API `start_date` for a window starting at [start] at [unit], or null when the window must not
     * send one.
     *
     * Only YEAR windows send it. Without a `start_date` the API does not anchor its year buckets to the
     * requested window, so a range over two years comes back with the wrong buckets and every number on
     * the card — chart, header total, % change and bottom row alike — is wrong. Sending the window's own
     * real start also truncates its first bucket to the range actually asked for, which is what keeps
     * the current and previous totals comparable: each then covers exactly its own day span. This
     * mirrors the web app, which requests `unit=year&date=<end>&start_date=<start>&quantity=<n>`.
     *
     * DAY and MONTH windows deliberately send none: they are already correct from unit + quantity +
     * endDate, and a mid-bucket start_date there truncates the first bucket's `views` (an additive
     * metric) while leaving `visitors` (a per-bucket unique) at the full-bucket value.
     */
    private fun apiStartDateOrNull(start: LocalDate, unit: StatsUnit): String? =
        if (unit == StatsUnit.YEAR) start.format(dateFormatter) else null

    /**
     * Fetches the bottom-row totals from a dedicated call, for the single-day periods only: Today and a
     * Custom range whose start and end are the same day. Their chart is hourly and an hourly response
     * only populates `views`, so the row can't be derived from [fetchStatsForPeriod].
     *
     * Every other period fills the row straight from the chart's own [fetchStatsForPeriod] response —
     * it requests the same unit, quantity and windows — and never calls this. The routing lives in the
     * view model's `fillsBottomFromChart`.
     *
     * Returns [BottomStatsResult.Error] (the row is hidden) only when a call errors or throws; a
     * successful-but-empty response is summed to a legitimate all-zero row so a genuine zero-traffic
     * period still shows totals.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Bottom-row totals for current and previous periods, or error
     */
    suspend fun fetchBottomStats(
        siteId: Long,
        period: StatsPeriod
    ): BottomStatsResult = withContext(ioDispatcher) {
        val bottomRange = calculateBottomStatsRange(period)

        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async {
                fetchBottomStatsVisits(
                    siteId = siteId,
                    unit = bottomRange.unit,
                    quantity = bottomRange.currentQuantity,
                    startDate = bottomRange.currentStart,
                    endDate = bottomRange.currentEnd
                )
            }
            val previousDeferred = async {
                fetchBottomStatsVisits(
                    siteId = siteId,
                    unit = bottomRange.unit,
                    quantity = bottomRange.previousQuantity,
                    startDate = bottomRange.previousStart,
                    endDate = bottomRange.previousEnd
                )
            }
            currentDeferred.await() to previousDeferred.await()
        }

        val current = bottomAggregatesOrNull(currentResult)
        val previous = bottomAggregatesOrNull(previousResult)
        if (current != null && previous != null) {
            BottomStatsResult.Success(current, previous)
        } else {
            BottomStatsResult.Error
        }
    }

    /**
     * Fetches the dedicated bottom-row stats for the [startDate]..[endDate] window, restricting the
     * response to the metrics shown in the bottom row.
     *
     * Takes the window explicitly rather than a [PeriodDateRange] so the current and previous calls
     * cannot accidentally share one window's quantity — they can span different bucket counts.
     *
     * The request mirrors the chart's [fetchStatsForPeriod] exactly, [apiStartDateOrNull] included: a
     * DAY or MONTH window is defined by unit + quantity + endDate alone, because a mid-bucket startDate
     * there makes the API truncate the first bucket's views (an additive metric) while leaving visitors
     * (a per-bucket unique) at the full-bucket value. A YEAR window must send its start.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchBottomStatsVisits(
        siteId: Long,
        unit: StatsUnit,
        quantity: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): StatsVisitsDataResult? {
        return try {
            statsDataSource.fetchStatsVisits(
                siteId = siteId,
                unit = unit,
                quantity = quantity,
                endDate = endDate.format(dateFormatter),
                startDate = apiStartDateOrNull(startDate, unit),
                statFields = CARD_STAT_FIELDS
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A bottom-stats failure must not cancel the sibling chart fetches or fail the whole
            // card; swallow it here and let the bottom row be hidden instead.
            appLogWrapper.e(AppLog.T.STATS, "Exception fetching bottom stats: ${e.message}")
            null
        }
    }

    private fun buildPeriodStatsSuccess(
        currentData: StatsVisitsData,
        previousData: StatsVisitsData,
        periodRange: PeriodDateRange
    ): PeriodStatsResult.Success {
        val currentDisplayDateString = periodRange.currentDisplayDate.format(dateFormatter)
        val previousDisplayDateString = periodRange.previousDisplayDate.format(dateFormatter)

        val currentAggregates = buildPeriodAggregates(
            currentData,
            periodRange.currentStart.format(dateFormatter),
            currentDisplayDateString
        )
        val previousAggregates = buildPeriodAggregates(
            previousData,
            periodRange.previousStart.format(dateFormatter),
            previousDisplayDateString
        )
        val currentPeriodData = currentData.toMetricDataPoints()
        val previousPeriodData = previousData.toMetricDataPoints()

        return PeriodStatsResult.Success(
            currentAggregates = currentAggregates,
            previousAggregates = previousAggregates,
            currentPeriodData = currentPeriodData,
            previousPeriodData = previousPeriodData,
            unit = periodRange.unit
        )
    }

    /**
     * Builds the bottom-row totals from a dedicated call result, or null when the row should be
     * hidden. Only a failed or thrown call (null result or error) hides the row; a successful
     * response is summed as-is, so a successful-but-empty response yields a legitimate all-zero row.
     */
    private fun bottomAggregatesOrNull(
        result: StatsVisitsDataResult?
    ): BottomStatsAggregates? = if (result is StatsVisitsDataResult.Success) {
        BottomStatsAggregates(
            views = result.data.visits.sumOf { it.visits },
            visitors = result.data.visitors.sumOf { it.visitors },
            likes = result.data.likes.sumOf { it.likes },
            comments = result.data.comments.sumOf { it.comments },
            posts = result.data.posts.sumOf { it.posts }
        )
    } else {
        null
    }

    private fun buildPeriodStatsError(
        currentResult: StatsVisitsDataResult,
        previousResult: StatsVisitsDataResult
    ): PeriodStatsResult.Error {
        val errorType = when {
            currentResult is StatsVisitsDataResult.Error ->
                currentResult.errorType
            previousResult is StatsVisitsDataResult.Error ->
                previousResult.errorType
            else -> StatsErrorType.UNKNOWN
        }
        appLogWrapper.e(
            AppLog.T.STATS,
            "API Error fetching period stats: $errorType"
        )
        return PeriodStatsResult.Error(errorType.name)
    }

    /**
     * Combines the parallel per-bucket lists in [StatsVisitsData] into one [ViewsDataPoint] per
     * bucket, so the chart can plot any of the five metrics from a single response. The metrics are
     * matched by their `period` key (rather than by list position) to stay correct even if a metric
     * omits a bucket; a metric with no entry for a bucket defaults to 0. The [visits] list drives the
     * bucket set, mirroring how it drives the chart's views series today.
     */
    private fun StatsVisitsData.toMetricDataPoints(): List<ViewsDataPoint> {
        val visitorsByPeriod = visitors.associate { it.period to it.visitors }
        val likesByPeriod = likes.associate { it.period to it.likes }
        val commentsByPeriod = comments.associate { it.period to it.comments }
        val postsByPeriod = posts.associate { it.period to it.posts }
        return visits.map { dataPoint ->
            ViewsDataPoint(
                period = dataPoint.period,
                views = dataPoint.visits,
                visitors = visitorsByPeriod[dataPoint.period] ?: 0L,
                likes = likesByPeriod[dataPoint.period] ?: 0L,
                comments = commentsByPeriod[dataPoint.period] ?: 0L,
                posts = postsByPeriod[dataPoint.period] ?: 0L
            )
        }
    }

    private fun buildPeriodAggregates(
        data: StatsVisitsData,
        startDate: String,
        endDate: String
    ): PeriodAggregates {
        return PeriodAggregates(
            views = data.visits.sumOf { it.visits },
            visitors = data.visitors.sumOf { it.visitors },
            likes = data.likes.sumOf { it.likes },
            comments = data.comments.sumOf { it.comments },
            posts = data.posts.sumOf { it.posts },
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * The two windows a card region compares, and the API bucket [unit] both are fetched at.
     *
     * Each window carries its own quantity. They are equal for every period except a Custom range
     * coarsened to MONTH or YEAR: the previous window mirrors the current one's exact *day* span (see
     * [previousWindowMirror]), which always resolves to the same unit but not always to the same number
     * of calendar buckets. Sending the current window's quantity on the previous request would make the
     * API return an extra leading bucket and fold it into the previous total, skewing the % change.
     */
    private data class PeriodDateRange(
        val currentStart: LocalDate,
        val currentEnd: LocalDate,
        val previousStart: LocalDate,
        val previousEnd: LocalDate,
        val currentQuantity: Int,
        val previousQuantity: Int,
        val unit: StatsUnit,
        // Display dates for the legend (may differ from API dates for hourly queries)
        val currentDisplayDate: LocalDate = currentEnd,
        val previousDisplayDate: LocalDate = previousEnd
    )

    private enum class DateUnit { DAY, MONTH }

    private data class PeriodConfig(val quantity: Int, val unit: StatsUnit, val dateUnit: DateUnit)

    /**
     * Computes the date range and API unit for a dedicated bottom-row fetch, coarsening the unit as the
     * span grows (day up to a month, month up to two years, year beyond) so the API de-duplicates
     * visitor uniques per bucket before the totals are summed. The previous window mirrors the current
     * one's exact day span ([previousWindowMirror], exactly as [calculateTodayPeriodDates] and
     * [calculateCustomPeriodDates] do), so the header's and the bottom row's Views % change compare
     * against identical windows and never disagree. Each window's quantity is derived from its own span
     * — the mirror can cover a different number of buckets.
     *
     * In practice only the single-day periods reach here (see [fetchBottomStats]), so the coarser units
     * are currently unreachable from the card — every longer period fills its row from the chart. The
     * span-based rule is kept so a caller that does need a standalone total for a longer window gets a
     * correct one rather than a silently wrong bucket.
     */
    private fun calculateBottomStatsRange(period: StatsPeriod): PeriodDateRange {
        val (currentStart, currentEnd) = currentPeriodWindow(period)
        val (unit, currentQuantity) = unitAndQuantityFor(currentStart, currentEnd)
        val (previousStart, previousEnd) = previousWindowMirror(currentStart, currentEnd)
        val (_, previousQuantity) = unitAndQuantityFor(previousStart, previousEnd)
        return PeriodDateRange(
            currentStart = currentStart,
            currentEnd = currentEnd,
            previousStart = previousStart,
            previousEnd = previousEnd,
            currentQuantity = currentQuantity,
            previousQuantity = previousQuantity,
            unit = unit
        )
    }

    /**
     * Chooses the API [StatsUnit] and its quantity for the inclusive real-calendar [start]..[end]
     * window, coarsening the bucket as the span grows: day up to a full month, month up to two years,
     * year beyond that. Shared by the bottom-row range and the custom-period chart window so the
     * thresholds and the quantity rule live in one place and the two always agree.
     *
     * Call it once per window — the quantity describes the window it was given, and a window and its
     * [previousWindowMirror] can span different numbers of buckets.
     *
     * The quantity counts the calendar buckets the window *spans*, not the whole units elapsed
     * between its endpoints: the API returns `quantity` buckets ending at the bucket holding the end
     * date, so counting elapsed units drops the window's first bucket whenever the end's day-of-month
     * falls before the start's (Jan 15..Mar 5 elapses 1 whole month but spans 3: Jan, Feb, Mar).
     */
    private fun unitAndQuantityFor(start: LocalDate, end: LocalDate): Pair<StatsUnit, Int> {
        val daysBetween = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val unit = when {
            daysBetween <= MAX_DAYS_IN_MONTH -> StatsUnit.DAY
            daysBetween > MAX_DAYS_IN_2_YEARS -> StatsUnit.YEAR
            else -> StatsUnit.MONTH
        }
        val quantity = when (unit) {
            StatsUnit.MONTH ->
                (ChronoUnit.MONTHS.between(start.withDayOfMonth(1), end.withDayOfMonth(1)).toInt() + 1)
                    .coerceAtLeast(1)
            StatsUnit.YEAR -> (end.year - start.year + 1).coerceAtLeast(1)
            else -> daysBetween
        }
        return unit to quantity
    }

    /**
     * The immediately-preceding window that mirrors [start]..[end]'s exact day span. Used only where
     * the chart itself compares against an exact day span (Today and Custom): for those paths the day
     * boundaries line up with the chart, so the Views % change stays consistent. Standard month-unit
     * periods instead align the previous window to whole months via [previousWindowForConfig], matching
     * the chart — a day-span mirror there would produce a different previous window and a contradictory
     * % change.
     *
     * The mirror preserves the day span, so [unitAndQuantityFor] always resolves it to the same unit as
     * the window it mirrors — but not necessarily to the same bucket count, so its quantity must be
     * derived from the mirror itself (2023-12-31..2026-01-01 spans 4 years; its mirror spans 3).
     */
    private fun previousWindowMirror(start: LocalDate, end: LocalDate): Pair<LocalDate, LocalDate> {
        val daysBetween = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val previousEnd = start.minusDays(1)
        val previousStart = previousEnd.minusDays((daysBetween - 1).toLong())
        return previousStart to previousEnd
    }

    /**
     * The previous window aligned to whole [PeriodConfig.dateUnit] steps (the chart's rule in
     * [calculatePeriodDates]): the previous window ends one unit before [currentStart] and spans
     * `quantity` units back. Shared so the chart and the bottom-row totals derive it identically.
     */
    private fun previousWindowForConfig(currentStart: LocalDate, config: PeriodConfig): Pair<LocalDate, LocalDate> {
        val previousEnd = subtractFromDate(currentStart, 1, config.dateUnit)
        val previousStart = subtractFromDate(previousEnd, config.quantity - 1, config.dateUnit)
        return previousStart to previousEnd
    }

    @Suppress("ReturnCount")
    private fun calculatePeriodDates(period: StatsPeriod): PeriodDateRange {
        if (period is StatsPeriod.Today) return calculateTodayPeriodDates()
        if (period is StatsPeriod.Custom) return calculateCustomPeriodDates(period.startDate, period.endDate)

        val config = getPeriodConfig(period)
        val (currentStart, currentEnd) = currentPeriodWindow(period)
        val (previousStart, previousEnd) = previousWindowForConfig(currentStart, config)

        // [previousWindowForConfig] spans exactly config.quantity units back, so both windows request
        // the same number of buckets by construction.
        return PeriodDateRange(
            currentStart = currentStart,
            currentEnd = currentEnd,
            previousStart = previousStart,
            previousEnd = previousEnd,
            currentQuantity = config.quantity,
            previousQuantity = config.quantity,
            unit = config.unit
        )
    }

    /**
     * The inclusive real-calendar current window (start..end) for a period. Shared by both the chart
     * window ([calculatePeriodDates]) and the bottom-row window ([calculateBottomStatsRange]) so the
     * current window is never derived from two different code paths.
     *
     * Note: [calculatePeriodDates] handles the hourly cases (Today and single-day Custom) separately
     * before reaching here, so those build their own window ending at "<day> 23:00:00".
     */
    private fun currentPeriodWindow(period: StatsPeriod): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            is StatsPeriod.Today -> today to today
            is StatsPeriod.Last7Days -> today.minusDays((DAYS_IN_7_DAYS - 1).toLong()) to today
            is StatsPeriod.Last30Days -> today.minusDays((DAYS_IN_30_DAYS - 1).toLong()) to today
            is StatsPeriod.Last6Months -> today.minusMonths((MONTHS_IN_6_MONTHS - 1).toLong()) to today
            is StatsPeriod.Last12Months -> today.minusMonths((MONTHS_IN_12_MONTHS - 1).toLong()) to today
            is StatsPeriod.Custom -> period.startDate to period.endDate
        }
    }

    private fun subtractFromDate(date: LocalDate, amount: Int, unit: DateUnit): LocalDate {
        return when (unit) {
            DateUnit.DAY -> date.minusDays(amount.toLong())
            DateUnit.MONTH -> date.minusMonths(amount.toLong())
        }
    }

    private fun getPeriodConfig(period: StatsPeriod): PeriodConfig = when (period) {
        is StatsPeriod.Last7Days -> PeriodConfig(DAYS_IN_7_DAYS, StatsUnit.DAY, DateUnit.DAY)
        is StatsPeriod.Last30Days -> PeriodConfig(DAYS_IN_30_DAYS, StatsUnit.DAY, DateUnit.DAY)
        is StatsPeriod.Last6Months -> PeriodConfig(MONTHS_IN_6_MONTHS, StatsUnit.MONTH, DateUnit.MONTH)
        is StatsPeriod.Last12Months -> PeriodConfig(MONTHS_IN_12_MONTHS, StatsUnit.MONTH, DateUnit.MONTH)
        else -> PeriodConfig(DAYS_IN_7_DAYS, StatsUnit.DAY, DateUnit.DAY) // Fallback to 7 days
    }

    /**
     * Calculates period dates for TODAY (hourly data). The hourly window ends at the day itself
     * ([formatApiEndDate] appends "23:00:00"), so the 24 buckets cover today's 00:00–23:00 and the
     * previous window covers yesterday's — matching the day-level totals shown in the bottom row.
     */
    private fun calculateTodayPeriodDates(): PeriodDateRange {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        return PeriodDateRange(
            currentStart = today,
            currentEnd = today,
            previousStart = yesterday,
            previousEnd = yesterday,
            currentQuantity = HOURLY_QUANTITY,
            previousQuantity = HOURLY_QUANTITY,
            unit = StatsUnit.HOUR,
            currentDisplayDate = today,
            previousDisplayDate = yesterday
        )
    }

    private fun calculateCustomPeriodDates(startDate: LocalDate, endDate: LocalDate): PeriodDateRange {
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        // Single day → hourly granularity (like Today). The hourly window ends at the day itself
        // ([formatApiEndDate] appends "23:00:00"), so the 24 buckets cover that day's 00:00–23:00.
        if (daysBetween == 1) {
            val previousDate = startDate.minusDays(1)
            return PeriodDateRange(
                currentStart = startDate,
                currentEnd = startDate,
                previousStart = previousDate,
                previousEnd = previousDate,
                currentQuantity = HOURLY_QUANTITY,
                previousQuantity = HOURLY_QUANTITY,
                unit = StatsUnit.HOUR,
                currentDisplayDate = startDate,
                previousDisplayDate = previousDate
            )
        }

        // Daily up to a full month (31 days), monthly up to two years, yearly beyond. Coarsening the
        // chart to YEAR past two years (the same rule the bottom-row totals use) keeps the chart and
        // the bottom row on one unit, so the header's Views and the bottom row's Views — and their
        // visitor de-duplication — always agree for long Custom ranges. A YEAR window also sends its
        // own start to the API (see [apiStartDateOrNull]).
        val (unit, currentQuantity) = unitAndQuantityFor(startDate, endDate)
        val (previousStart, previousEnd) = previousWindowMirror(startDate, endDate)
        val (_, previousQuantity) = unitAndQuantityFor(previousStart, previousEnd)

        return PeriodDateRange(
            currentStart = startDate,
            currentEnd = endDate,
            previousStart = previousStart,
            previousEnd = previousEnd,
            currentQuantity = currentQuantity,
            previousQuantity = previousQuantity,
            unit = unit
        )
    }

    /**
     * Calculates the start and end dates for a given week.
     *
     * @param weeksAgo Number of weeks to go back (0 = current week, 1 = previous week)
     * @return Pair of (startDate, endDate) LocalDates representing the 7-day period
     */
    private fun calculateWeekDateRange(weeksAgo: Int): Pair<LocalDate, LocalDate> {
        val endDate = LocalDate.now().minusWeeks(weeksAgo.toLong())
        val startDate = endDate.plusDays(DAYS_BEFORE_END_DATE.toLong())
        return startDate to endDate
    }

    /**
     * Fetches most viewed items based on the selected data source and period.
     * Also fetches the previous period data to calculate change comparisons.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @param dataSource The data source type (posts and pages or referrers)
     * @return Most viewed items with comparison data or error
     */
    @Suppress("ReturnCount")
    suspend fun fetchMostViewed(
        siteId: Long,
        period: StatsPeriod,
        dataSource: MostViewedDataSource
    ): MostViewedResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) = calculateComparisonDateRanges(period)

        when (dataSource) {
            MostViewedDataSource.POSTS_AND_PAGES -> {
                fetchTopPostsWithComparison(siteId, currentDateRange, previousDateRange)
            }
            MostViewedDataSource.REFERRERS -> {
                fetchReferrersWithComparison(siteId, currentDateRange, previousDateRange, REFERRERS_CARD_MAX)
            }
        }
    }

    /**
     * Fetches the full referrer list (max = 0 = unlimited) for the referrers detail screen. Kept
     * separate from [fetchMostViewed] (which bounds the card to [REFERRERS_CARD_MAX]) so the detail
     * screen can show more entries than the card without inflating the card request.
     */
    suspend fun fetchReferrersDetail(
        siteId: Long,
        period: StatsPeriod
    ): MostViewedResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) = calculateComparisonDateRanges(period)
        fetchReferrersWithComparison(siteId, currentDateRange, previousDateRange, REFERRERS_DETAIL_MAX)
    }

    private suspend fun fetchTopPostsWithComparison(
        siteId: Long,
        currentDateRange: StatsDateRange,
        previousDateRange: StatsDateRange
    ): MostViewedResult = coroutineScope {
        appLogWrapper.d(
            AppLog.T.STATS,
            "StatsRepository: fetchTopPostsWithComparison - siteId=$siteId, " +
                "currentDateRange=$currentDateRange, previousDateRange=$previousDateRange"
        )

        val currentDeferred = async { statsDataSource.fetchTopPostsAndPages(siteId, currentDateRange) }
        val previousDeferred = async { statsDataSource.fetchTopPostsAndPages(siteId, previousDateRange) }

        val currentResult = currentDeferred.await()
        val previousResult = previousDeferred.await()

        appLogWrapper.d(
            AppLog.T.STATS,
            "StatsRepository: fetchTopPostsWithComparison results - " +
                "currentResult=${currentResult::class.simpleName}, previousResult=${previousResult::class.simpleName}"
        )

        if (currentResult is TopPostsDataResult.Success) {
            val previousItemsMap = if (previousResult is TopPostsDataResult.Success) {
                previousResult.items.associateBy { it.id }
            } else {
                emptyMap()
            }

            val totalViews = currentResult.items.sumOf { it.views }
            val previousTotalViews = previousItemsMap.values.sumOf { it.views }
            val totalChange = totalViews - previousTotalViews
            val totalChangePercent = if (previousTotalViews > 0) {
                (totalChange.toDouble() / previousTotalViews.toDouble()) * PERCENTAGE_MULTIPLIER
            } else if (totalViews > 0) PERCENTAGE_MULTIPLIER else PERCENTAGE_NO_CHANGE

            MostViewedResult.Success(
                items = currentResult.items.mapIndexed { index, item ->
                    val previousViews = previousItemsMap[item.id]?.views ?: 0L
                    MostViewedItemData(
                        id = item.id,
                        title = item.title,
                        views = item.views,
                        previousViews = previousViews,
                        isFirst = index == 0,
                        url = item.url,
                        postType = item.postType
                    )
                },
                totalViews = totalViews,
                totalViewsChange = totalChange,
                totalViewsChangePercent = totalChangePercent
            )
        } else {
            val error = currentResult as TopPostsDataResult.Error
            appLogWrapper.e(
                AppLog.T.STATS,
                "Error fetching top posts: ${error.errorType}"
            )
            MostViewedResult.Error(error.errorType.name)
        }
    }

    private suspend fun fetchReferrersWithComparison(
        siteId: Long,
        currentDateRange: StatsDateRange,
        previousDateRange: StatsDateRange,
        max: Int
    ): MostViewedResult = coroutineScope {
        // The card requests REFERRERS_CARD_MAX items; the detail screen requests all of them by
        // passing max = 0 (the server treats 0 as "unlimited", vs. an unset max that defaults to 10).
        // Fetching all only when the detail screen opens keeps the card request small and avoids
        // passing a large list across the process boundary.
        val currentDeferred = async { statsDataSource.fetchReferrers(siteId, currentDateRange, max = max) }
        val previousDeferred = async { statsDataSource.fetchReferrers(siteId, previousDateRange, max = max) }

        val currentResult = currentDeferred.await()
        val previousResult = previousDeferred.await()

        if (currentResult is ReferrersDataResult.Success) {
            val previousItemsMap = if (previousResult is ReferrersDataResult.Success) {
                previousResult.items.associateBy { it.name }
            } else {
                emptyMap()
            }

            val totalViews = currentResult.items.sumOf { it.views }
            val previousTotalViews = previousItemsMap.values.sumOf { it.views }
            val totalChange = totalViews - previousTotalViews
            val totalChangePercent = if (previousTotalViews > 0) {
                (totalChange.toDouble() / previousTotalViews.toDouble()) * PERCENTAGE_MULTIPLIER
            } else if (totalViews > 0) PERCENTAGE_MULTIPLIER else PERCENTAGE_NO_CHANGE

            MostViewedResult.Success(
                items = currentResult.items.mapIndexed { index, item ->
                    val previousViews = previousItemsMap[item.name]?.views ?: 0L
                    MostViewedItemData(
                        id = item.name.hashCode().toLong(),
                        title = item.name,
                        views = item.views,
                        previousViews = previousViews,
                        isFirst = index == 0,
                        children = item.children.map { child ->
                            MostViewedChildData(
                                name = child.name,
                                url = child.url,
                                views = child.views
                            )
                        },
                        url = item.url
                    )
                },
                totalViews = totalViews,
                totalViewsChange = totalChange,
                totalViewsChangePercent = totalChangePercent
            )
        } else {
            val error = currentResult as ReferrersDataResult.Error
            appLogWrapper.e(
                AppLog.T.STATS,
                "Error fetching referrers: ${error.errorType}"
            )
            MostViewedResult.Error(error.errorType.name)
        }
    }

    /**
     * Calculates current and previous date ranges for comparison stats.
     * Used by multiple stats types (MostViewed, Countries, etc.)
     */
    private fun calculateCurrentDateRange(period: StatsPeriod): StatsDateRange {
        val today = LocalDate.now()
        val todayString = today.format(dateFormatter)
        return when (period) {
            is StatsPeriod.Today ->
                StatsDateRange.Preset(num = NUM_DAYS_TODAY, date = todayString)
            is StatsPeriod.Last7Days ->
                StatsDateRange.Preset(num = DAYS_IN_7_DAYS, date = todayString)
            is StatsPeriod.Last30Days ->
                StatsDateRange.Preset(num = DAYS_IN_30_DAYS, date = todayString)
            is StatsPeriod.Last6Months ->
                StatsDateRange.Preset(num = DAYS_IN_6_MONTHS, date = todayString)
            is StatsPeriod.Last12Months ->
                StatsDateRange.Preset(num = DAYS_IN_12_MONTHS, date = todayString)
            is StatsPeriod.Custom ->
                StatsDateRange.Custom(
                    startDate = period.startDate.format(dateFormatter),
                    date = period.endDate.format(dateFormatter)
                )
        }
    }

    private fun calculateComparisonDateRanges(period: StatsPeriod): Pair<StatsDateRange, StatsDateRange> {
        val today = LocalDate.now()
        val todayString = today.format(dateFormatter)

        return when (period) {
            is StatsPeriod.Today -> {
                val yesterdayString = today.minusDays(NUM_DAYS_TODAY.toLong()).format(dateFormatter)
                StatsDateRange.Preset(num = NUM_DAYS_TODAY, date = todayString) to
                    StatsDateRange.Preset(num = NUM_DAYS_TODAY, date = yesterdayString)
            }
            is StatsPeriod.Last7Days -> {
                val previousEndString = today.minusDays(DAYS_IN_7_DAYS.toLong()).format(dateFormatter)
                StatsDateRange.Preset(num = DAYS_IN_7_DAYS, date = todayString) to
                    StatsDateRange.Preset(num = DAYS_IN_7_DAYS, date = previousEndString)
            }
            is StatsPeriod.Last30Days -> {
                val previousEndString = today.minusDays(DAYS_IN_30_DAYS.toLong()).format(dateFormatter)
                StatsDateRange.Preset(num = DAYS_IN_30_DAYS, date = todayString) to
                    StatsDateRange.Preset(num = DAYS_IN_30_DAYS, date = previousEndString)
            }
            is StatsPeriod.Last6Months -> {
                val previousEndString = today.minusDays(DAYS_IN_6_MONTHS.toLong()).format(dateFormatter)
                StatsDateRange.Preset(num = DAYS_IN_6_MONTHS, date = todayString) to
                    StatsDateRange.Preset(num = DAYS_IN_6_MONTHS, date = previousEndString)
            }
            is StatsPeriod.Last12Months -> {
                val previousEndString = today.minusDays(DAYS_IN_12_MONTHS.toLong()).format(dateFormatter)
                StatsDateRange.Preset(num = DAYS_IN_12_MONTHS, date = todayString) to
                    StatsDateRange.Preset(num = DAYS_IN_12_MONTHS, date = previousEndString)
            }
            is StatsPeriod.Custom -> {
                val (previousStart, previousEnd) = previousWindowMirror(period.startDate, period.endDate)
                StatsDateRange.Custom(
                    startDate = period.startDate.format(dateFormatter),
                    date = period.endDate.format(dateFormatter)
                ) to StatsDateRange.Custom(
                    startDate = previousStart.format(dateFormatter),
                    date = previousEnd.format(dateFormatter)
                )
            }
        }
    }

    /**
     * Fetches country views stats for a specific site and period with comparison data.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Country views data with comparison or error
     */
    suspend fun fetchCountryViews(
        siteId: Long,
        period: StatsPeriod
    ): CountryViewsResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) = calculateComparisonDateRanges(period)

        // Fetch both periods in parallel
        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async { statsDataSource.fetchCountryViews(siteId, currentDateRange) }
            val previousDeferred = async { statsDataSource.fetchCountryViews(siteId, previousDateRange) }
            currentDeferred.await() to previousDeferred.await()
        }

        when (currentResult) {
            is CountryViewsDataResult.Success -> {
                val previousCountriesMap = if (previousResult is CountryViewsDataResult.Success) {
                    previousResult.data.countries.associateBy { it.countryCode }
                } else {
                    emptyMap()
                }

                // Calculate totalViews from countries list (API summary.totalViews may be null/0)
                val totalViews = currentResult.data.countries.sumOf { it.views }
                val previousTotalViews = if (previousResult is CountryViewsDataResult.Success) {
                    previousResult.data.countries.sumOf { it.views }
                } else {
                    0L
                }
                val totalChange = totalViews - previousTotalViews
                val totalChangePercent = if (previousTotalViews > 0) {
                    (totalChange.toDouble() / previousTotalViews.toDouble()) * PERCENTAGE_MULTIPLIER
                } else if (totalViews > 0) PERCENTAGE_MULTIPLIER else PERCENTAGE_NO_CHANGE

                CountryViewsResult.Success(
                    countries = currentResult.data.countries.map { country ->
                        val previousViews = previousCountriesMap[country.countryCode]?.views ?: 0L
                        CountryViewItemData(
                            countryCode = country.countryCode,
                            countryName = country.countryName,
                            views = country.views,
                            flagIconUrl = country.flagIconUrl,
                            previousViews = previousViews
                        )
                    },
                    totalViews = totalViews,
                    otherViews = currentResult.data.otherViews,
                    totalViewsChange = totalChange,
                    totalViewsChangePercent = totalChangePercent
                )
            }
            is CountryViewsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching country views: " +
                        "${currentResult.errorType}"
                )
                CountryViewsResult.Error(
                    currentResult.errorType.messageResId,
                    currentResult.errorType == StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    /**
     * Fetches region views stats for a specific site and period with comparison data.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Region views data with comparison or error
     */
    suspend fun fetchRegionViews(
        siteId: Long,
        period: StatsPeriod
    ): RegionViewsResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) =
            calculateComparisonDateRanges(period)

        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async {
                statsDataSource.fetchRegionViews(siteId, currentDateRange)
            }
            val previousDeferred = async {
                statsDataSource.fetchRegionViews(siteId, previousDateRange)
            }
            currentDeferred.await() to previousDeferred.await()
        }

        when (currentResult) {
            is RegionViewsDataResult.Success -> {
                buildRegionViewsSuccess(currentResult, previousResult)
            }
            is RegionViewsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching region views: " +
                        "${currentResult.errorType}"
                )
                RegionViewsResult.Error(
                    currentResult.errorType.messageResId,
                    currentResult.errorType == StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    private fun buildRegionViewsSuccess(
        currentResult: RegionViewsDataResult.Success,
        previousResult: RegionViewsDataResult
    ): RegionViewsResult {
        val previousMap =
            if (previousResult is RegionViewsDataResult.Success) {
                previousResult.data.regions.associateBy { it.location }
            } else {
                emptyMap()
            }

        val totalViews = currentResult.data.regions.sumOf { it.views }
        val previousTotalViews =
            if (previousResult is RegionViewsDataResult.Success) {
                previousResult.data.regions.sumOf { it.views }
            } else {
                0L
            }
        val totalChange = totalViews - previousTotalViews
        val totalChangePercent = calculateChangePercent(
            totalViews, previousTotalViews, totalChange
        )

        return RegionViewsResult.Success(
            regions = currentResult.data.regions.map { region ->
                val prev = previousMap[region.location]?.views ?: 0L
                RegionViewItemData(
                    location = region.location,
                    countryCode = region.countryCode,
                    views = region.views,
                    flagIconUrl = region.flagIconUrl,
                    previousViews = prev
                )
            },
            totalViews = totalViews,
            otherViews = currentResult.data.otherViews,
            totalViewsChange = totalChange,
            totalViewsChangePercent = totalChangePercent
        )
    }

    /**
     * Fetches city views stats for a specific site and period with comparison data.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return City views data with comparison or error
     */
    suspend fun fetchCityViews(
        siteId: Long,
        period: StatsPeriod
    ): CityViewsResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) =
            calculateComparisonDateRanges(period)

        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async {
                statsDataSource.fetchCityViews(siteId, currentDateRange)
            }
            val previousDeferred = async {
                statsDataSource.fetchCityViews(siteId, previousDateRange)
            }
            currentDeferred.await() to previousDeferred.await()
        }

        when (currentResult) {
            is CityViewsDataResult.Success -> {
                buildCityViewsSuccess(currentResult, previousResult)
            }
            is CityViewsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching city views: " +
                        "${currentResult.errorType}"
                )
                CityViewsResult.Error(
                    currentResult.errorType.messageResId,
                    currentResult.errorType == StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    private fun buildCityViewsSuccess(
        currentResult: CityViewsDataResult.Success,
        previousResult: CityViewsDataResult
    ): CityViewsResult {
        val previousMap =
            if (previousResult is CityViewsDataResult.Success) {
                previousResult.data.cities.associateBy { it.location }
            } else {
                emptyMap()
            }

        val totalViews = currentResult.data.cities.sumOf { it.views }
        val previousTotalViews =
            if (previousResult is CityViewsDataResult.Success) {
                previousResult.data.cities.sumOf { it.views }
            } else {
                0L
            }
        val totalChange = totalViews - previousTotalViews
        val totalChangePercent = calculateChangePercent(
            totalViews, previousTotalViews, totalChange
        )

        return CityViewsResult.Success(
            cities = currentResult.data.cities.map { city ->
                val prev = previousMap[city.location]?.views ?: 0L
                CityViewItemData(
                    location = city.location,
                    countryCode = city.countryCode,
                    views = city.views,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    flagIconUrl = city.flagIconUrl,
                    previousViews = prev
                )
            },
            totalViews = totalViews,
            otherViews = currentResult.data.otherViews,
            totalViewsChange = totalChange,
            totalViewsChangePercent = totalChangePercent
        )
    }

    /**
     * Fetches top authors stats for a specific site and period with comparison data.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Top authors data with comparison or error
     */
    suspend fun fetchTopAuthors(
        siteId: Long,
        period: StatsPeriod
    ): TopAuthorsResult = withContext(ioDispatcher) {
        val (currentDateRange, previousDateRange) = calculateComparisonDateRanges(period)

        // Fetch both periods in parallel
        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async { statsDataSource.fetchTopAuthors(siteId, currentDateRange, max = 0) }
            val previousDeferred = async { statsDataSource.fetchTopAuthors(siteId, previousDateRange, max = 0) }
            currentDeferred.await() to previousDeferred.await()
        }

        when (currentResult) {
            is TopAuthorsDataResult.Success -> {
                val previousAuthorsMap = if (previousResult is TopAuthorsDataResult.Success) {
                    previousResult.data.authors.associateBy { it.name }
                } else {
                    emptyMap()
                }

                val totalViews = currentResult.data.authors.sumOf { it.views }
                val previousTotalViews = if (previousResult is TopAuthorsDataResult.Success) {
                    previousResult.data.authors.sumOf { it.views }
                } else {
                    0L
                }
                val totalChange = totalViews - previousTotalViews
                val totalChangePercent = if (previousTotalViews > 0) {
                    (totalChange.toDouble() / previousTotalViews.toDouble()) * PERCENTAGE_MULTIPLIER
                } else if (totalViews > 0) PERCENTAGE_MULTIPLIER else PERCENTAGE_NO_CHANGE

                TopAuthorsResult.Success(
                    authors = currentResult.data.authors.map { author ->
                        val previousViews = previousAuthorsMap[author.name]?.views ?: 0L
                        TopAuthorItemData(
                            name = author.name,
                            avatarUrl = author.avatarUrl,
                            views = author.views,
                            previousViews = previousViews
                        )
                    },
                    totalViews = totalViews,
                    totalViewsChange = totalChange,
                    totalViewsChangePercent = totalChangePercent
                )
            }
            is TopAuthorsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching top authors: " +
                        "${currentResult.errorType}"
                )
                TopAuthorsResult.Error(
                    currentResult.errorType.messageResId,
                    currentResult.errorType == StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    suspend fun fetchClicks(
        siteId: Long,
        period: StatsPeriod
    ): ClicksResult = fetchWithComparison(
        period = period,
        fetch = { dateRange ->
            when (val r = statsDataSource.fetchClicks(
                siteId, dateRange, max = 0
            )) {
                is ClicksDataResult.Success ->
                    DataSourceResult.Success(r.items)
                is ClicksDataResult.Error ->
                    DataSourceResult.Error(r.errorType)
            }
        },
        keyOf = { it.name },
        metricOf = { it.clicks },
        mapItem = { item, prev ->
            ClickItemData(
                name = item.name,
                clicks = item.clicks,
                previousClicks = prev,
                url = item.url,
                children = item.children.map { child ->
                    MostViewedChildData(
                        name = child.name,
                        url = child.url,
                        views = child.clicks
                    )
                }
            )
        },
        buildSuccess = { items, total, change, pct ->
            ClicksResult.Success(
                items, total, change, pct
            )
        },
        buildError = { resId, isAuth, isNotAvailable ->
            ClicksResult.Error(resId, isAuth, isNotAvailable)
        },
        logLabel = "clicks"
    )

    suspend fun fetchSearchTerms(
        siteId: Long,
        period: StatsPeriod
    ): SearchTermsResult = fetchWithComparison(
        period = period,
        fetch = { dateRange ->
            when (val r = statsDataSource.fetchSearchTerms(
                siteId, dateRange, max = 0
            )) {
                is SearchTermsDataResult.Success ->
                    DataSourceResult.Success(r.items)
                is SearchTermsDataResult.Error ->
                    DataSourceResult.Error(r.errorType)
            }
        },
        keyOf = { it.name },
        metricOf = { it.views },
        mapItem = { item, prev ->
            SearchTermItemData(item.name, item.views, prev)
        },
        buildSuccess = { items, total, change, pct ->
            SearchTermsResult.Success(
                items, total, change, pct
            )
        },
        buildError = { resId, isAuth, isNotAvailable ->
            SearchTermsResult.Error(resId, isAuth, isNotAvailable)
        },
        logLabel = "search terms"
    )

    suspend fun fetchVideoPlays(
        siteId: Long,
        period: StatsPeriod
    ): VideoPlaysResult = fetchWithComparison(
        period = period,
        fetch = { dateRange ->
            when (val r = statsDataSource.fetchVideoPlays(
                siteId, dateRange, max = 0
            )) {
                is VideoPlaysDataResult.Success ->
                    DataSourceResult.Success(r.items)
                is VideoPlaysDataResult.Error ->
                    DataSourceResult.Error(r.errorType)
            }
        },
        keyOf = { it.title },
        metricOf = { it.views },
        mapItem = { item, prev ->
            VideoPlayItemData(item.title, item.views, prev)
        },
        buildSuccess = { items, total, change, pct ->
            VideoPlaysResult.Success(
                items, total, change, pct
            )
        },
        buildError = { resId, isAuth, isNotAvailable ->
            VideoPlaysResult.Error(resId, isAuth, isNotAvailable)
        },
        logLabel = "video plays"
    )

    suspend fun fetchFileDownloads(
        siteId: Long,
        period: StatsPeriod
    ): FileDownloadsResult = fetchWithComparison(
        period = period,
        fetch = { dateRange ->
            when (val r = statsDataSource.fetchFileDownloads(
                siteId, dateRange, max = 0
            )) {
                is FileDownloadsDataResult.Success ->
                    DataSourceResult.Success(r.items)
                is FileDownloadsDataResult.Error ->
                    DataSourceResult.Error(r.errorType)
            }
        },
        keyOf = { it.name },
        metricOf = { it.downloads },
        mapItem = { item, prev ->
            FileDownloadItemData(
                item.name, item.downloads, prev
            )
        },
        buildSuccess = { items, total, change, pct ->
            FileDownloadsResult.Success(
                items, total, change, pct
            )
        },
        buildError = { resId, isAuth, isNotAvailable ->
            FileDownloadsResult.Error(resId, isAuth, isNotAvailable)
        },
        logLabel = "file downloads"
    )

    /**
     * Fetches device screen size stats for a specific site and period.
     */
    suspend fun fetchDevicesScreensize(
        siteId: Long,
        period: StatsPeriod
    ): DevicesResult = withContext(ioDispatcher) {
        val dateRange = calculateCurrentDateRange(period)
        fetchDevicesData { statsDataSource.fetchDevicesScreensize(siteId, dateRange) }
    }

    /**
     * Fetches device browser stats for a specific site and period.
     */
    suspend fun fetchDevicesBrowser(
        siteId: Long,
        period: StatsPeriod
    ): DevicesResult = withContext(ioDispatcher) {
        val dateRange = calculateCurrentDateRange(period)
        fetchDevicesData { statsDataSource.fetchDevicesBrowser(siteId, dateRange) }
    }

    /**
     * Fetches device platform stats for a specific site and period.
     */
    suspend fun fetchDevicesPlatform(
        siteId: Long,
        period: StatsPeriod
    ): DevicesResult = withContext(ioDispatcher) {
        val dateRange = calculateCurrentDateRange(period)
        fetchDevicesData { statsDataSource.fetchDevicesPlatform(siteId, dateRange) }
    }

    private suspend fun fetchDevicesData(
        fetch: suspend () -> DevicesDataResult
    ): DevicesResult {
        return when (val result = fetch()) {
            is DevicesDataResult.Success -> {
                val items = result.data.items
                    .map { (name, views) ->
                        DeviceItemData(
                            name = name,
                            views = views
                        )
                    }
                    .sortedByDescending { it.views }
                DevicesResult.Success(items = items)
            }
            is DevicesDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching devices: ${result.errorType}"
                )
                DevicesResult.Error(
                    result.errorType.messageResId,
                    result.errorType == StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    /**
     * Fetches UTM stats for a specific site and period.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchUtm(
        siteId: Long,
        keys: List<String>,
        period: StatsPeriod
    ): UtmResult = withContext(ioDispatcher) {
        val curRange =
            calculateCurrentDateRange(period)
        val curResult = statsDataSource.fetchUtm(
            siteId, keys,
            curRange.dateString(),
            curRange.daysCount()
        )
        when (curResult) {
            is UtmDataResult.Success -> {
                val curValues =
                    curResult.data.topUtmValues
                val total = curValues.values.sum()
                UtmResult.Success(
                    items = curValues.entries
                        .sortedByDescending {
                            it.value
                        }
                        .map { (name, views) ->
                            val posts = curResult
                                .data.topPosts[name]
                                .orEmpty()
                            UtmItemData(
                                name = name,
                                views = views,
                                topPosts = posts.map {
                                    UtmPostItemData(
                                        it.title,
                                        it.views
                                    )
                                }
                            )
                        },
                    totalViews = total
                )
            }
            is UtmDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching UTM: " +
                        "${curResult.errorType}"
                )
                UtmResult.Error(
                    curResult.errorType.messageResId,
                    curResult.errorType ==
                        StatsErrorType.AUTH_ERROR
                )
            }
        }
    }

    private fun StatsDateRange.dateString(): String =
        when (this) {
            is StatsDateRange.Preset -> date
            is StatsDateRange.Custom -> date
        }

    private fun StatsDateRange.daysCount(): Int =
        when (this) {
            is StatsDateRange.Preset -> num
            is StatsDateRange.Custom -> {
                val start = LocalDate.parse(
                    startDate, dateFormatter
                )
                val end = LocalDate.parse(
                    date, dateFormatter
                )
                ChronoUnit.DAYS.between(start, end)
                    .toInt()
                    .coerceAtLeast(1)
            }
        }

    private fun calculateChangePercent(
        totalViews: Long,
        previousTotalViews: Long,
        totalChange: Long
    ): Double = if (previousTotalViews > 0) {
        (totalChange.toDouble() / previousTotalViews.toDouble()) *
            PERCENTAGE_MULTIPLIER
    } else if (totalViews > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }

    /**
     * Generic helper that encapsulates the parallel
     * fetch-with-comparison pattern used by multiple stats
     * endpoints. Fetches current and previous period data in
     * parallel, builds a lookup map for comparison, and
     * delegates result construction to the caller via lambdas.
     */
    @Suppress("LongParameterList")
    private suspend fun <Raw, Output, R> fetchWithComparison(
        period: StatsPeriod,
        fetch: suspend (StatsDateRange) -> DataSourceResult<Raw>,
        keyOf: (Raw) -> String,
        metricOf: (Raw) -> Long,
        mapItem: (Raw, Long) -> Output,
        buildSuccess: (List<Output>, Long, Long, Double) -> R,
        buildError: (Int, Boolean, Boolean) -> R,
        logLabel: String
    ): R = withContext(ioDispatcher) {
        val (curRange, prevRange) =
            calculateComparisonDateRanges(period)

        val (curResult, prevResult) = coroutineScope {
            val c = async { fetch(curRange) }
            val p = async { fetch(prevRange) }
            c.await() to p.await()
        }

        when (curResult) {
            is DataSourceResult.Success -> {
                val prevMap =
                    if (prevResult is DataSourceResult.Success) {
                        prevResult.items.associateBy(keyOf)
                    } else {
                        emptyMap()
                    }
                val total = curResult.items.sumOf(metricOf)
                val prevTotal = prevMap.values.sumOf(metricOf)
                val change = total - prevTotal
                val changePct = calculateChangePercent(
                    total, prevTotal, change
                )
                buildSuccess(
                    curResult.items.map { item ->
                        val prev = prevMap[keyOf(item)]
                            ?.let(metricOf) ?: 0L
                        mapItem(item, prev)
                    },
                    total, change, changePct
                )
            }
            is DataSourceResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching $logLabel: " +
                        "${curResult.errorType}"
                )
                buildError(
                    curResult.errorType.messageResId,
                    curResult.errorType ==
                        StatsErrorType.AUTH_ERROR,
                    curResult.errorType ==
                        StatsErrorType.NOT_AVAILABLE
                )
            }
        }
    }

    suspend fun fetchInsights(
        siteId: Long
    ): InsightsResult = withContext(ioDispatcher) {
        val result = statsDataSource.fetchStatsInsights(
            siteId = siteId
        )
        when (result) {
            is StatsInsightsDataResult.Success ->
                InsightsResult.Success(
                    data = result.data
                )
            is StatsInsightsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching insights: " +
                        "${result.errorType}"
                )
                InsightsResult.Error(
                    result.errorType.name
                )
            }
        }
    }

    suspend fun fetchStatsSummary(
        siteId: Long
    ): StatsSummaryResult = withContext(ioDispatcher) {
        val result =
            statsDataSource.fetchStatsSummary(
                siteId = siteId
            )
        when (result) {
            is StatsSummaryDataResult.Success ->
                StatsSummaryResult.Success(
                    data = result.data
                )
            is StatsSummaryDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching stats " +
                        "summary: " +
                        "${result.errorType}"
                )
                StatsSummaryResult.Error(
                    result.errorType.name
                )
            }
        }
    }

    suspend fun fetchTags(
        siteId: Long,
        max: Int = DEFAULT_TAGS_MAX
    ): TagsResult = withContext(ioDispatcher) {
        val result = statsDataSource.fetchStatsTags(
            siteId = siteId,
            max = max
        )
        when (result) {
            is StatsTagsDataResult.Success ->
                TagsResult.Success(
                    data = result.data
                )
            is StatsTagsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "Error fetching tags: " +
                        "${result.errorType}"
                )
                TagsResult.Error(
                    result.errorType.name
                )
            }
        }
    }

    /**
     * Fetches all-time subscriber counts: current, 30d ago,
     * 60d ago, 90d ago. Makes 4 parallel API calls.
     */
    @Suppress("MagicNumber")
    suspend fun fetchSubscribersAllTime(
        siteId: Long
    ): SubscribersAllTimeResult = withContext(ioDispatcher) {
        val results = fetchAllTimeResults(siteId)

        val firstError = results.filterIsInstance<
            StatsSubscribersDataResult.Error>().firstOrNull()
        if (firstError != null) {
            return@withContext SubscribersAllTimeResult.Error(
                messageResId =
                    firstError.errorType.messageResId,
                isAuthError = firstError.errorType ==
                    StatsErrorType.AUTH_ERROR
            )
        }

        val current = results[0]
        val d30 = results[1]
        val d60 = results[2]
        val d90 = results[3]
        SubscribersAllTimeResult.Success(
            currentCount = extractSubscriberCount(current),
            count30DaysAgo = extractSubscriberCount(d30),
            count60DaysAgo = extractSubscriberCount(d60),
            count90DaysAgo = extractSubscriberCount(d90)
        )
    }

    @Suppress("MagicNumber")
    private suspend fun fetchAllTimeResults(
        siteId: Long
    ): List<StatsSubscribersDataResult> {
        val today = java.time.LocalDate.now()
        val dateFormat =
            java.time.format.DateTimeFormatter
                .ISO_LOCAL_DATE
        val dates = listOf(0L, 30L, 60L, 90L).map {
            today.minusDays(it).format(dateFormat)
        }
        return coroutineScope {
            dates.map { date ->
                async {
                    statsDataSource.fetchStatsSubscribers(
                        siteId,
                        quantity = 1,
                        date = date
                    )
                }
            }.map { it.await() }
        }
    }

    private fun extractSubscriberCount(
        result: StatsSubscribersDataResult
    ): Long {
        val data = (result as?
            StatsSubscribersDataResult.Success)?.data
            ?: return 0L
        return data.subscribersData
            .firstOrNull()?.count ?: 0L
    }

    /**
     * Fetches subscriber graph data for a given time unit.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchSubscribersGraph(
        siteId: Long,
        unit: String,
        quantity: Int,
        date: String
    ): SubscribersGraphResult = withContext(ioDispatcher) {
        try {
            when (
                val result = statsDataSource
                    .fetchStatsSubscribers(
                        siteId,
                        quantity = quantity,
                        unit = unit,
                        date = date
                    )
            ) {
                is StatsSubscribersDataResult.Success -> {
                    SubscribersGraphResult.Success(
                        dataPoints =
                            result.data.subscribersData.map {
                                SubscribersGraphDataPoint(
                                    date = it.date,
                                    count = it.count
                                )
                            }
                    )
                }
                is StatsSubscribersDataResult.Error -> {
                    SubscribersGraphResult.Error(
                        messageResId =
                            result.errorType.messageResId,
                        isAuthError = result.errorType ==
                            StatsErrorType.AUTH_ERROR
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching subscribers graph",
                e
            )
            SubscribersGraphResult.Error(
                messageResId = R.string.stats_error_api
            )
        }
    }

    /**
     * Fetches a list of subscribers for the given site.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchSubscribersList(
        siteId: Long,
        perPage: Int = SUBSCRIBERS_DEFAULT_MAX,
        page: Int = 1
    ): SubscribersListResult = withContext(ioDispatcher) {
        try {
            when (
                val result = statsDataSource
                    .fetchSubscribersByUserType(
                        siteId, perPage, page
                    )
            ) {
                is SubscribersByUserTypeDataResult
                    .Success -> {
                    SubscribersListResult.Success(
                        subscribers =
                            result.items.map {
                                SubscriberItemData(
                                    displayName =
                                        it.displayName,
                                    subscribedSince =
                                        it.subscribedSince
                                )
                            }
                    )
                }
                is SubscribersByUserTypeDataResult
                    .Error -> {
                    SubscribersListResult.Error(
                        messageResId =
                            result.errorType.messageResId,
                        isAuthError =
                            result.errorType ==
                                StatsErrorType.AUTH_ERROR
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching subscribers list",
                e
            )
            SubscribersListResult.Error(
                messageResId = R.string.stats_error_api
            )
        }
    }

    /**
     * Fetches email stats summary for the given site.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchEmailsSummary(
        siteId: Long,
        quantity: Int = SUBSCRIBERS_DEFAULT_MAX
    ): EmailsStatsResult = withContext(ioDispatcher) {
        try {
            when (
                val result = statsDataSource
                    .fetchStatsEmailsSummary(
                        siteId, quantity
                    )
            ) {
                is StatsEmailsSummaryDataResult
                    .Success -> {
                    EmailsStatsResult.Success(
                        items = result.items.map {
                            EmailItemData(
                                title = it.title,
                                opens = it.opens,
                                clicks = it.clicks
                            )
                        }
                    )
                }
                is StatsEmailsSummaryDataResult
                    .Error -> {
                    EmailsStatsResult.Error(
                        messageResId =
                            result.errorType.messageResId,
                        isAuthError =
                            result.errorType ==
                                StatsErrorType.AUTH_ERROR
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching emails summary",
                e
            )
            EmailsStatsResult.Error(
                messageResId = R.string.stats_error_api
            )
        }
    }

    companion object {
        private const val DEFAULT_TAGS_MAX = 10
    }
}

/**
 * Intermediate sealed class that normalises data-source
 * results into a common shape for [StatsRepository.fetchWithComparison].
 */
private sealed class DataSourceResult<out T> {
    data class Success<T>(
        val items: List<T>
    ) : DataSourceResult<T>()
    data class Error(
        val errorType: StatsErrorType
    ) : DataSourceResult<Nothing>()
}

/**
 * Result wrapper for hourly views fetch operation.
 */
sealed class HourlyViewsResult {
    data class Success(val dataPoints: List<HourlyViewsDataPoint>) : HourlyViewsResult()
    data class Error(val message: String) : HourlyViewsResult()
}

/**
 * Raw data point from the stats API.
 */
data class HourlyViewsDataPoint(
    val period: String,
    val views: Long
)

/**
 * Result wrapper for today's aggregated stats fetch operation.
 */
sealed class TodayAggregatesResult {
    data class Success(val aggregates: TodayAggregates) : TodayAggregatesResult()
    data class Error(val message: String) : TodayAggregatesResult()
}

/**
 * Today's aggregated stats data.
 */
data class TodayAggregates(
    val views: Long,
    val visitors: Long,
    val likes: Long,
    val comments: Long
)

/**
 * Result wrapper for weekly aggregated stats fetch operation.
 */
sealed class WeeklyStatsResult {
    data class Success(val aggregates: PeriodAggregates) : WeeklyStatsResult()
    data class Error(val message: String) : WeeklyStatsResult()
}

/**
 * Aggregated stats data for a period.
 */
data class PeriodAggregates(
    val views: Long,
    val visitors: Long,
    val likes: Long,
    val comments: Long,
    val posts: Long,
    val startDate: String,
    val endDate: String
)

/**
 * Result wrapper for daily views fetch operation.
 */
sealed class DailyViewsResult {
    data class Success(val dataPoints: List<ViewsDataPoint>) : DailyViewsResult()
    data class Error(val message: String) : DailyViewsResult()
}

/**
 * A data point from the stats API representing one time bucket (hour, day, or month).
 *
 * Carries all five card metrics for that bucket. A non-hourly `stats/visits` response provides every
 * metric per bucket, so the chart can plot any of them without a new network call. An hourly
 * (single-day) response only populates [views]; the other four default to 0 and are never charted
 * (the ViewModel disables non-views selection on single-day periods).
 */
data class ViewsDataPoint(
    val period: String,
    val views: Long,
    val visitors: Long = 0L,
    val likes: Long = 0L,
    val comments: Long = 0L,
    val posts: Long = 0L
)

/**
 * Result wrapper for combined weekly stats fetch operation.
 * Contains both aggregated stats and daily data points from a single API call.
 */
sealed class WeeklyStatsWithDailyDataResult {
    data class Success(
        val aggregates: PeriodAggregates,
        val dailyDataPoints: List<ViewsDataPoint>
    ) : WeeklyStatsWithDailyDataResult()
    data class Error(val message: String) : WeeklyStatsWithDailyDataResult()
}

/**
 * Result wrapper for period stats fetch operation.
 * Contains aggregated stats and data points for both current and previous periods.
 *
 * [unit] is the granularity the buckets in [currentPeriodData]/[previousPeriodData] were requested
 * at. It is carried here because the bucket's [ViewsDataPoint.period] string alone cannot express it
 * — the API returns a full ISO date for DAY, MONTH and YEAR buckets alike, so a YEAR bucket is
 * indistinguishable from a day without this.
 */
sealed class PeriodStatsResult {
    data class Success(
        val currentAggregates: PeriodAggregates,
        val previousAggregates: PeriodAggregates,
        val currentPeriodData: List<ViewsDataPoint>,
        val previousPeriodData: List<ViewsDataPoint>,
        val unit: StatsUnit
    ) : PeriodStatsResult()
    data class Error(val message: String) : PeriodStatsResult()
}

/**
 * Bottom-row totals for a period. Unlike [PeriodAggregates] it carries no date fields, because the
 * bottom row renders only the counts and the current-vs-previous change.
 */
data class BottomStatsAggregates(
    val views: Long,
    val visitors: Long,
    val likes: Long,
    val comments: Long,
    val posts: Long
)

/**
 * Result wrapper for the dedicated bottom-row totals fetch. [Error] means the row should be hidden
 * (a call failed or threw); a successful-but-empty response is still a [Success] with zero counts.
 */
sealed class BottomStatsResult {
    data class Success(
        val current: BottomStatsAggregates,
        val previous: BottomStatsAggregates
    ) : BottomStatsResult()
    data object Error : BottomStatsResult()
}

/**
 * Result wrapper for most viewed fetch operation.
 */
sealed class MostViewedResult {
    data class Success(
        val items: List<MostViewedItemData>,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : MostViewedResult()
    data class Error(val message: String) : MostViewedResult()
}

/**
 * Data for a single most viewed item from the repository layer.
 */
data class MostViewedItemData(
    val id: Long,
    val title: String,
    val views: Long,
    val previousViews: Long,
    val isFirst: Boolean,
    val children: List<MostViewedChildData> = emptyList(),
    val url: String? = null,
    val postType: String? = null
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

/**
 * A child item nested under a most viewed item (e.g. a referrer under a referrer group).
 */
data class MostViewedChildData(
    val name: String,
    val url: String?,
    val views: Long
)

/**
 * Result wrapper for country views fetch operation.
 */
sealed class CountryViewsResult {
    data class Success(
        val countries: List<CountryViewItemData>,
        val totalViews: Long,
        val otherViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : CountryViewsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : CountryViewsResult()
}

/**
 * Data for a single country view item from the repository layer.
 */
data class CountryViewItemData(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

/**
 * Result wrapper for region views fetch operation.
 */
sealed class RegionViewsResult {
    data class Success(
        val regions: List<RegionViewItemData>,
        val totalViews: Long,
        val otherViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : RegionViewsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : RegionViewsResult()
}

/**
 * Data for a single region view item from the repository layer.
 */
data class RegionViewItemData(
    val location: String,
    val countryCode: String,
    val views: Long,
    val flagIconUrl: String?,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

/**
 * Result wrapper for city views fetch operation.
 */
sealed class CityViewsResult {
    data class Success(
        val cities: List<CityViewItemData>,
        val totalViews: Long,
        val otherViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : CityViewsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : CityViewsResult()
}

/**
 * Data for a single city view item from the repository layer.
 */
data class CityViewItemData(
    val location: String,
    val countryCode: String,
    val views: Long,
    val latitude: String?,
    val longitude: String?,
    val flagIconUrl: String?,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

/**
 * Result wrapper for top authors fetch operation.
 */
sealed class TopAuthorsResult {
    data class Success(
        val authors: List<TopAuthorItemData>,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : TopAuthorsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : TopAuthorsResult()
}

/**
 * Data for a single top author item from the repository layer.
 */
data class TopAuthorItemData(
    val name: String,
    val avatarUrl: String?,
    val views: Long,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

sealed class ClicksResult {
    data class Success(
        val items: List<ClickItemData>,
        val totalClicks: Long,
        val totalClicksChange: Long,
        val totalClicksChangePercent: Double
    ) : ClicksResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : ClicksResult()
}

data class ClickItemData(
    val name: String,
    val clicks: Long,
    val previousClicks: Long,
    val url: String? = null,
    val children: List<MostViewedChildData> = emptyList()
) {
    val clicksChange: Long get() = clicks - previousClicks
    val clicksChangePercent: Double
        get() = calculateItemChangePercent(clicks, previousClicks)
}

sealed class SearchTermsResult {
    data class Success(
        val items: List<SearchTermItemData>,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : SearchTermsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : SearchTermsResult()
}

data class SearchTermItemData(
    val name: String,
    val views: Long,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

sealed class VideoPlaysResult {
    data class Success(
        val items: List<VideoPlayItemData>,
        val totalViews: Long,
        val totalViewsChange: Long,
        val totalViewsChangePercent: Double
    ) : VideoPlaysResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : VideoPlaysResult()
}

data class VideoPlayItemData(
    val title: String,
    val views: Long,
    val previousViews: Long
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double
        get() = calculateItemChangePercent(views, previousViews)
}

sealed class FileDownloadsResult {
    data class Success(
        val items: List<FileDownloadItemData>,
        val totalDownloads: Long,
        val totalDownloadsChange: Long,
        val totalDownloadsChangePercent: Double
    ) : FileDownloadsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : FileDownloadsResult()
}

data class FileDownloadItemData(
    val name: String,
    val downloads: Long,
    val previousDownloads: Long
) {
    val downloadsChange: Long get() = downloads - previousDownloads
    val downloadsChangePercent: Double
        get() = calculateItemChangePercent(
            downloads, previousDownloads
        )
}

/**
 * Result wrapper for devices stats fetch operation.
 */
sealed class DevicesResult {
    data class Success(
        val items: List<DeviceItemData>
    ) : DevicesResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : DevicesResult()
}

/**
 * Data for a single device item from the repository layer.
 */
data class DeviceItemData(
    val name: String,
    val views: Double
)

/**
 * Result of fetching insights data from the repository.
 */
sealed class InsightsResult {
    data class Success(
        val data: StatsInsightsData
    ) : InsightsResult()
    data class Error(
        val message: String
    ) : InsightsResult()
}

/**
 * Result of fetching stats summary data from the repository.
 */
sealed class StatsSummaryResult {
    data class Success(
        val data: StatsSummaryData
    ) : StatsSummaryResult()
    data class Error(
        val message: String
    ) : StatsSummaryResult()
}

/**
 * Result of fetching tags data from the repository.
 */
sealed class TagsResult {
    data class Success(
        val data: StatsTagsData
    ) : TagsResult()
    data class Error(
        val message: String
    ) : TagsResult()
}

/**
 * Result wrapper for subscribers all-time stats fetch operation.
 */
sealed class SubscribersAllTimeResult {
    data class Success(
        val currentCount: Long,
        val count30DaysAgo: Long,
        val count60DaysAgo: Long,
        val count90DaysAgo: Long
    ) : SubscribersAllTimeResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : SubscribersAllTimeResult()
}

/**
 * Result wrapper for subscribers list fetch operation.
 */
sealed class SubscribersListResult {
    data class Success(
        val subscribers: List<SubscriberItemData>
    ) : SubscribersListResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : SubscribersListResult()
}

/**
 * Data for a single subscriber item from the repository layer.
 */
data class SubscriberItemData(
    val displayName: String,
    val subscribedSince: String
)

/**
 * Result wrapper for emails stats fetch operation.
 */
sealed class EmailsStatsResult {
    data class Success(
        val items: List<EmailItemData>
    ) : EmailsStatsResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : EmailsStatsResult()
}

/**
 * Data for a single email item from the repository layer.
 */
data class EmailItemData(
    val title: String,
    val opens: Long,
    val clicks: Long
)

/**
 * Result wrapper for subscribers graph fetch operation.
 */
sealed class SubscribersGraphResult {
    data class Success(
        val dataPoints: List<SubscribersGraphDataPoint>
    ) : SubscribersGraphResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : SubscribersGraphResult()
}

/**
 * A single data point for the subscribers graph.
 */
data class SubscribersGraphDataPoint(
    val date: String,
    val count: Long
)

/**
 * Result wrapper for UTM stats fetch operation.
 */
sealed class UtmResult {
    data class Success(
        val items: List<UtmItemData>,
        val totalViews: Long
    ) : UtmResult()
    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : UtmResult()
}

/**
 * Data for a single UTM item from the repository layer.
 */
data class UtmItemData(
    val name: String,
    val views: Long,
    val topPosts: List<UtmPostItemData>
)

/**
 * Data for a single post within a UTM item.
 */
data class UtmPostItemData(
    val title: String,
    val views: Long
)
