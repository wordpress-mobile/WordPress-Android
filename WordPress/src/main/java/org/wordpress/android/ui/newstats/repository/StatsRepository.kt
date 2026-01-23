package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.util.AppLog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

private const val HOURLY_QUANTITY = 24
private const val DAILY_QUANTITY = 1
private const val WEEKLY_QUANTITY = 7
private const val DAYS_BEFORE_END_DATE = -6
private const val DAYS_IN_30_DAYS = 30
private const val DAYS_IN_7_DAYS = 7
private const val MONTHS_IN_6_MONTHS = 6
private const val MONTHS_IN_12_MONTHS = 12

/**
 * Repository for fetching stats data using the wordpress-rs API.
 * Handles hourly visits/views data for the Today's Stats card chart.
 */
class StatsRepository @Inject constructor(
    private val statsDataSource: StatsDataSource,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Thread-local date formatter for thread-safe date formatting.
     * SimpleDateFormat is NOT thread-safe, so we use ThreadLocal to provide each thread
     * with its own instance, avoiding the overhead of creating new instances on every call.
     */
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    }

    private fun getDateFormat(): SimpleDateFormat = dateFormat.get()!!

    fun init(accessToken: String) {
        statsDataSource.init(accessToken)
    }

    /**
     * Fetches today's aggregated stats (views, visitors, likes, comments).
     *
     * @param siteId The WordPress.com site ID
     * @return Today's aggregated stats or error
     */
    suspend fun fetchTodayAggregates(siteId: Long): TodayAggregatesResult = withContext(ioDispatcher) {
        val calendar = Calendar.getInstance()
        val dateString = getDateFormat().format(calendar.time)

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
                appLogWrapper.e(AppLog.T.STATS, "API Error fetching today aggregates: ${result.message}")
                TodayAggregatesResult.Error(result.message)
            }
        }
    }

    /**
     * Fetches hourly views data for the specified date.
     *
     * @param siteId The WordPress.com site ID
     * @param offsetDays Number of days to offset from today (0 = today, 1 = yesterday, etc.)
     * @return List of hourly views data points, or empty list if fetch fails
     */
    suspend fun fetchHourlyViews(
        siteId: Long,
        offsetDays: Int = 0
    ): HourlyViewsResult = withContext(ioDispatcher) {
        val calendar = Calendar.getInstance()
        // The API's endDate is exclusive for hourly queries, so we need to add 1 day to get
        // the target day's hours. Formula: 1 (for exclusive end) - offsetDays (0=today, 1=yesterday)
        // Examples: offsetDays=0 → tomorrow's date → fetches today's hours
        //           offsetDays=1 → today's date → fetches yesterday's hours
        calendar.add(Calendar.DAY_OF_YEAR, 1 - offsetDays)
        val dateString = getDateFormat().format(calendar.time)

        val result = statsDataSource.fetchStatsVisits(
            siteId = siteId,
            unit = StatsUnit.HOUR,
            quantity = HOURLY_QUANTITY,
            endDate = dateString
        )

        when (result) {
            is StatsVisitsDataResult.Success -> {
                val dataPoints = result.data.visits.map { dataPoint ->
                    HourlyViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
                }
                HourlyViewsResult.Success(dataPoints)
            }

            is StatsVisitsDataResult.Error -> {
                appLogWrapper.e(AppLog.T.STATS, "API Error fetching hourly views: ${result.message}")
                HourlyViewsResult.Error(result.message)
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
            val endDateString = getDateFormat().format(endDate.time)

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

                    val startDateFormatted = getDateFormat().format(startDate.time)

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
                    appLogWrapper.e(AppLog.T.STATS, "API Error fetching weekly stats: ${result.message}")
                    WeeklyStatsResult.Error(result.message)
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
            val endDateString = getDateFormat().format(endDate.time)

            val result = statsDataSource.fetchStatsVisits(
                siteId = siteId,
                unit = StatsUnit.DAY,
                quantity = WEEKLY_QUANTITY,
                endDate = endDateString
            )

            when (result) {
                is StatsVisitsDataResult.Success -> {
                    val dataPoints = result.data.visits.map { dataPoint ->
                        ViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
                    }
                    DailyViewsResult.Success(dataPoints)
                }

                is StatsVisitsDataResult.Error -> {
                    appLogWrapper.e(AppLog.T.STATS, "API Error fetching daily views: ${result.message}")
                    DailyViewsResult.Error(result.message)
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
        val endDateString = getDateFormat().format(endDate.time)

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
                val startDateFormatted = getDateFormat().format(startDate.time)

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
                val dailyDataPoints = data.visits.map { dataPoint ->
                    ViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
                }

                WeeklyStatsWithDailyDataResult.Success(aggregates, dailyDataPoints)
            }

            is StatsVisitsDataResult.Error -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "API Error fetching weekly stats with daily data: ${result.message}"
                )
                WeeklyStatsWithDailyDataResult.Error(result.message)
            }
        }
    }

    /**
     * Fetches stats data for a specific period with comparison to the previous period.
     *
     * @param siteId The WordPress.com site ID
     * @param period The stats period to fetch
     * @return Combined stats for current and previous periods or error
     */
    suspend fun fetchStatsForPeriod(
        siteId: Long,
        period: StatsPeriod
    ): PeriodStatsResult = withContext(ioDispatcher) {
        val periodRange = calculatePeriodDates(period)

        val currentEndString = getDateFormat().format(periodRange.currentEnd.time)
        val previousEndString = getDateFormat().format(periodRange.previousEnd.time)

        // Fetch both periods in parallel for better performance
        val (currentResult, previousResult) = coroutineScope {
            val currentDeferred = async {
                statsDataSource.fetchStatsVisits(
                    siteId = siteId,
                    unit = periodRange.unit,
                    quantity = periodRange.quantity,
                    endDate = currentEndString
                )
            }
            val previousDeferred = async {
                statsDataSource.fetchStatsVisits(
                    siteId = siteId,
                    unit = periodRange.unit,
                    quantity = periodRange.quantity,
                    endDate = previousEndString
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

    private fun buildPeriodStatsSuccess(
        currentData: StatsVisitsData,
        previousData: StatsVisitsData,
        periodRange: PeriodDateRange
    ): PeriodStatsResult.Success {
        val dateFormat = getDateFormat()
        val currentDisplayDateString = dateFormat.format(periodRange.currentDisplayDate.time)
        val previousDisplayDateString = dateFormat.format(periodRange.previousDisplayDate.time)

        val currentAggregates = buildPeriodAggregates(
            currentData,
            dateFormat.format(periodRange.currentStart.time),
            currentDisplayDateString
        )
        val previousAggregates = buildPeriodAggregates(
            previousData,
            dateFormat.format(periodRange.previousStart.time),
            previousDisplayDateString
        )
        val currentPeriodData = currentData.visits.map { dataPoint ->
            ViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
        }
        val previousPeriodData = previousData.visits.map { dataPoint ->
            ViewsDataPoint(period = dataPoint.period, views = dataPoint.visits)
        }

        return PeriodStatsResult.Success(
            currentAggregates = currentAggregates,
            previousAggregates = previousAggregates,
            currentPeriodData = currentPeriodData,
            previousPeriodData = previousPeriodData
        )
    }

    private fun buildPeriodStatsError(
        currentResult: StatsVisitsDataResult,
        previousResult: StatsVisitsDataResult
    ): PeriodStatsResult.Error {
        val errorMessage = when {
            currentResult is StatsVisitsDataResult.Error -> currentResult.message
            previousResult is StatsVisitsDataResult.Error -> previousResult.message
            else -> "Unknown error"
        }
        appLogWrapper.e(AppLog.T.STATS, "API Error fetching period stats: $errorMessage")
        return PeriodStatsResult.Error(errorMessage)
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

    private data class PeriodDateRange(
        val currentStart: Calendar,
        val currentEnd: Calendar,
        val previousStart: Calendar,
        val previousEnd: Calendar,
        val quantity: Int,
        val unit: StatsUnit,
        // Display dates for the legend (may differ from API dates for hourly queries)
        val currentDisplayDate: Calendar = currentEnd,
        val previousDisplayDate: Calendar = previousEnd
    )

    private data class PeriodConfig(val quantity: Int, val unit: StatsUnit, val calendarField: Int)

    @Suppress("ReturnCount")
    private fun calculatePeriodDates(period: StatsPeriod): PeriodDateRange {
        if (period is StatsPeriod.Today) return calculateTodayPeriodDates()
        if (period is StatsPeriod.Custom) return calculateCustomPeriodDates(period.startDate, period.endDate)

        val config = getPeriodConfig(period)
        val currentEnd = Calendar.getInstance()
        val currentStart = (currentEnd.clone() as Calendar).apply {
            add(config.calendarField, -(config.quantity - 1))
        }
        val previousEnd = (currentStart.clone() as Calendar).apply {
            add(config.calendarField, -1)
        }
        val previousStart = (previousEnd.clone() as Calendar).apply {
            add(config.calendarField, -(config.quantity - 1))
        }
        return PeriodDateRange(currentStart, currentEnd, previousStart, previousEnd, config.quantity, config.unit)
    }

    private fun getPeriodConfig(period: StatsPeriod): PeriodConfig = when (period) {
        is StatsPeriod.Last7Days -> PeriodConfig(DAYS_IN_7_DAYS, StatsUnit.DAY, Calendar.DAY_OF_YEAR)
        is StatsPeriod.Last30Days -> PeriodConfig(DAYS_IN_30_DAYS, StatsUnit.DAY, Calendar.DAY_OF_YEAR)
        is StatsPeriod.Last6Months -> PeriodConfig(MONTHS_IN_6_MONTHS, StatsUnit.MONTH, Calendar.MONTH)
        is StatsPeriod.Last12Months -> PeriodConfig(MONTHS_IN_12_MONTHS, StatsUnit.MONTH, Calendar.MONTH)
        else -> PeriodConfig(DAYS_IN_7_DAYS, StatsUnit.DAY, Calendar.DAY_OF_YEAR) // Fallback to 7 days
    }

    /**
     * Calculates period dates for TODAY (hourly data).
     * The API's endDate is exclusive for hourly queries, so we use tomorrow as end date for today's hours.
     */
    private fun calculateTodayPeriodDates(): PeriodDateRange {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return PeriodDateRange(
            currentStart = today,
            currentEnd = tomorrow,
            previousStart = yesterday,
            previousEnd = today,
            quantity = HOURLY_QUANTITY,
            unit = StatsUnit.HOUR,
            currentDisplayDate = today,
            previousDisplayDate = yesterday
        )
    }

    private fun calculateCustomPeriodDates(startDate: LocalDate, endDate: LocalDate): PeriodDateRange {
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        // Convert LocalDate to Calendar
        val currentStart = localDateToCalendar(startDate)
        val currentEnd = localDateToCalendar(endDate)

        // Calculate previous period with same duration
        val previousEnd = (currentStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val previousStart = (previousEnd.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -(daysBetween - 1))
        }

        // Determine unit based on range
        val unit = when {
            daysBetween <= DAYS_IN_30_DAYS -> StatsUnit.DAY
            else -> StatsUnit.MONTH
        }

        val quantity = if (unit == StatsUnit.MONTH) {
            // Calculate months between dates
            val monthsBetween = ChronoUnit.MONTHS.between(startDate, endDate).toInt() + 1
            monthsBetween.coerceAtLeast(1)
        } else {
            daysBetween
        }

        return PeriodDateRange(
            currentStart = currentStart,
            currentEnd = currentEnd,
            previousStart = previousStart,
            previousEnd = previousEnd,
            quantity = quantity,
            unit = unit
        )
    }

    private fun localDateToCalendar(localDate: LocalDate): Calendar {
        return Calendar.getInstance().apply {
            time = java.util.Date.from(
                localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        }
    }

    /**
     * Calculates the start and end dates for a given week.
     *
     * @param weeksAgo Number of weeks to go back (0 = current week, 1 = previous week)
     * @return Pair of (startDate, endDate) Calendars representing the 7-day period
     */
    private fun calculateWeekDateRange(weeksAgo: Int): Pair<Calendar, Calendar> {
        val endDate = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        }

        val startDate = (endDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, DAYS_BEFORE_END_DATE)
        }

        return startDate to endDate
    }
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
 * A data point from the stats API representing views for a time unit (hour, day, or month).
 */
data class ViewsDataPoint(
    val period: String,
    val views: Long
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
 */
sealed class PeriodStatsResult {
    data class Success(
        val currentAggregates: PeriodAggregates,
        val previousAggregates: PeriodAggregates,
        val currentPeriodData: List<ViewsDataPoint>,
        val previousPeriodData: List<ViewsDataPoint>
    ) : PeriodStatsResult()
    data class Error(val message: String) : PeriodStatsResult()
}
