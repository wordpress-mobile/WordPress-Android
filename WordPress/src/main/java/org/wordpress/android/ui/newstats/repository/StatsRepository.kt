package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.ui.newstats.datasource.CityViewsDataResult
import org.wordpress.android.ui.newstats.datasource.CountryViewsDataResult
import org.wordpress.android.ui.newstats.datasource.ReferrersDataResult
import org.wordpress.android.ui.newstats.datasource.RegionViewsDataResult
import org.wordpress.android.ui.newstats.datasource.StatsDataSource
import org.wordpress.android.ui.newstats.datasource.StatsDateRange
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.datasource.StatsVisitsData
import org.wordpress.android.ui.newstats.datasource.StatsVisitsDataResult
import org.wordpress.android.ui.newstats.datasource.TopAuthorsDataResult
import org.wordpress.android.ui.newstats.datasource.TopPostsDataResult
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.util.AppLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named

private const val HOURLY_QUANTITY = 24
private const val DAILY_QUANTITY = 1
private const val WEEKLY_QUANTITY = 7
private const val DAYS_BEFORE_END_DATE = -6
private const val DAYS_IN_7_DAYS = 7
private const val DAYS_IN_30_DAYS = 30
private const val DAYS_IN_6_MONTHS = 182
private const val DAYS_IN_12_MONTHS = 365
private const val MONTHS_IN_6_MONTHS = 6
private const val MONTHS_IN_12_MONTHS = 12
private const val PERCENTAGE_MULTIPLIER = 100.0
private const val PERCENTAGE_NO_CHANGE = 0.0
private const val NUM_DAYS_TODAY = 1

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
        // The API's endDate is exclusive for hourly queries, so we need to add 1 day to get
        // the target day's hours. Formula: 1 (for exclusive end) - offsetDays (0=today, 1=yesterday)
        // Examples: offsetDays=0 → tomorrow's date → fetches today's hours
        //           offsetDays=1 → today's date → fetches yesterday's hours
        val dateString = LocalDate.now().plusDays((1 - offsetDays).toLong()).format(dateFormatter)

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
            val endDateString = endDate.format(dateFormatter)

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

        val currentEndString = periodRange.currentEnd.format(dateFormatter)
        val previousEndString = periodRange.previousEnd.format(dateFormatter)

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
        val currentStart: LocalDate,
        val currentEnd: LocalDate,
        val previousStart: LocalDate,
        val previousEnd: LocalDate,
        val quantity: Int,
        val unit: StatsUnit,
        // Display dates for the legend (may differ from API dates for hourly queries)
        val currentDisplayDate: LocalDate = currentEnd,
        val previousDisplayDate: LocalDate = previousEnd
    )

    private enum class DateUnit { DAY, MONTH }

    private data class PeriodConfig(val quantity: Int, val unit: StatsUnit, val dateUnit: DateUnit)

    @Suppress("ReturnCount")
    private fun calculatePeriodDates(period: StatsPeriod): PeriodDateRange {
        if (period is StatsPeriod.Today) return calculateTodayPeriodDates()
        if (period is StatsPeriod.Custom) return calculateCustomPeriodDates(period.startDate, period.endDate)

        val config = getPeriodConfig(period)
        val currentEnd = LocalDate.now()
        val currentStart = subtractFromDate(currentEnd, config.quantity - 1, config.dateUnit)
        val previousEnd = subtractFromDate(currentStart, 1, config.dateUnit)
        val previousStart = subtractFromDate(previousEnd, config.quantity - 1, config.dateUnit)

        return PeriodDateRange(currentStart, currentEnd, previousStart, previousEnd, config.quantity, config.unit)
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
     * Calculates period dates for TODAY (hourly data).
     * The API's endDate is exclusive for hourly queries, so we use tomorrow as end date for today's hours.
     */
    private fun calculateTodayPeriodDates(): PeriodDateRange {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val yesterday = today.minusDays(1)
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

        val previousEnd = startDate.minusDays(1)
        val previousStart = previousEnd.minusDays(daysBetween.toLong() - 1)

        // Determine unit based on range
        val unit = when {
            daysBetween <= DAYS_IN_30_DAYS -> StatsUnit.DAY
            else -> StatsUnit.MONTH
        }

        val quantity = if (unit == StatsUnit.MONTH) {
            val monthsBetween = ChronoUnit.MONTHS.between(startDate, endDate).toInt() + 1
            monthsBetween.coerceAtLeast(1)
        } else {
            daysBetween
        }

        return PeriodDateRange(
            currentStart = startDate,
            currentEnd = endDate,
            previousStart = previousStart,
            previousEnd = previousEnd,
            quantity = quantity,
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
                fetchReferrersWithComparison(siteId, currentDateRange, previousDateRange)
            }
        }
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
                        isFirst = index == 0
                    )
                },
                totalViews = totalViews,
                totalViewsChange = totalChange,
                totalViewsChangePercent = totalChangePercent
            )
        } else {
            val error = currentResult as TopPostsDataResult.Error
            appLogWrapper.e(AppLog.T.STATS, "Error fetching top posts: ${error.message}")
            MostViewedResult.Error(error.message)
        }
    }

    private suspend fun fetchReferrersWithComparison(
        siteId: Long,
        currentDateRange: StatsDateRange,
        previousDateRange: StatsDateRange
    ): MostViewedResult = coroutineScope {
        val currentDeferred = async { statsDataSource.fetchReferrers(siteId, currentDateRange) }
        val previousDeferred = async { statsDataSource.fetchReferrers(siteId, previousDateRange) }

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
                        isFirst = index == 0
                    )
                },
                totalViews = totalViews,
                totalViewsChange = totalChange,
                totalViewsChangePercent = totalChangePercent
            )
        } else {
            val error = currentResult as ReferrersDataResult.Error
            appLogWrapper.e(AppLog.T.STATS, "Error fetching referrers: ${error.message}")
            MostViewedResult.Error(error.message)
        }
    }

    /**
     * Calculates current and previous date ranges for comparison stats.
     * Used by multiple stats types (MostViewed, Countries, etc.)
     */
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
                val daysBetween = ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
                val previousEnd = period.startDate.minusDays(1)
                val previousStart = previousEnd.minusDays(daysBetween.toLong() - 1)
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
                appLogWrapper.e(AppLog.T.STATS, "Error fetching country views: ${currentResult.message}")
                CountryViewsResult.Error(currentResult.message)
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
                        currentResult.message
                )
                RegionViewsResult.Error(currentResult.message)
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
                        currentResult.message
                )
                CityViewsResult.Error(currentResult.message)
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
                appLogWrapper.e(AppLog.T.STATS, "Error fetching top authors: ${currentResult.message}")
                TopAuthorsResult.Error(currentResult.message)
            }
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
    val isFirst: Boolean
) {
    val viewsChange: Long get() = views - previousViews
    val viewsChangePercent: Double get() = if (previousViews > 0) {
        (viewsChange.toDouble() / previousViews.toDouble()) * PERCENTAGE_MULTIPLIER
    } else if (views > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
}

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
    data class Error(val message: String) : CountryViewsResult()
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
    val viewsChangePercent: Double get() = if (previousViews > 0) {
        (viewsChange.toDouble() / previousViews.toDouble()) * PERCENTAGE_MULTIPLIER
    } else if (views > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
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
    data class Error(val message: String) : RegionViewsResult()
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
    val viewsChangePercent: Double get() = if (previousViews > 0) {
        (viewsChange.toDouble() / previousViews.toDouble()) * PERCENTAGE_MULTIPLIER
    } else if (views > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
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
    data class Error(val message: String) : CityViewsResult()
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
    val viewsChangePercent: Double get() = if (previousViews > 0) {
        (viewsChange.toDouble() / previousViews.toDouble()) * PERCENTAGE_MULTIPLIER
    } else if (views > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
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
    data class Error(val message: String) : TopAuthorsResult()
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
    val viewsChangePercent: Double get() = if (previousViews > 0) {
        (viewsChange.toDouble() / previousViews.toDouble()) * PERCENTAGE_MULTIPLIER
    } else if (views > 0) {
        PERCENTAGE_MULTIPLIER
    } else {
        PERCENTAGE_NO_CHANGE
    }
}
