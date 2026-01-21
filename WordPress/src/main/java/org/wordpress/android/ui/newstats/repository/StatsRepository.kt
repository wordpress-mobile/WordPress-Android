package org.wordpress.android.ui.newstats.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.ui.newstats.extension.statsCommentsData
import org.wordpress.android.ui.newstats.extension.statsLikesData
import org.wordpress.android.ui.newstats.extension.statsPostsData
import org.wordpress.android.ui.newstats.extension.statsVisitorsData
import org.wordpress.android.ui.newstats.extension.statsVisitsData
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsVisitsParams
import uniffi.wp_api.StatsVisitsUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

private const val HOURLY_QUANTITY = 24u
private const val DAILY_QUANTITY = 1u
private const val WEEKLY_QUANTITY = 7u
private const val DAYS_BEFORE_END_DATE = -6

/**
 * Repository for fetching stats data using the wordpress-rs API.
 * Handles hourly visits/views data for the Today's Stats card chart.
 */
class StatsRepository @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Access token for API authentication.
     * Marked as @Volatile to ensure visibility across threads since this repository is accessed
     * from multiple coroutine contexts (main thread initialization, IO dispatcher for API calls).
     */
    @Volatile
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null) { "Repository not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

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
        this.accessToken = accessToken
    }

    /**
     * Fetches today's aggregated stats (views, visitors, likes, comments).
     *
     * @param siteId The WordPress.com site ID
     * @return Today's aggregated stats or error
     */
    suspend fun fetchTodayAggregates(siteId: Long): TodayAggregatesResult = withContext(ioDispatcher) {
        if (accessToken == null) {
            appLogWrapper.e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
            return@withContext TodayAggregatesResult.Error("Repository not initialized")
        }

        val calendar = Calendar.getInstance()
        val dateString = getDateFormat().format(calendar.time)

        val params = StatsVisitsParams(
            unit = StatsVisitsUnit.DAY,
            quantity = DAILY_QUANTITY,
            endDate = dateString,
        )

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        when (result) {
            is WpRequestResult.Success -> {
                val response = result.response.data
                val views = response.statsVisitsData().firstOrNull()?.visits?.toLong() ?: 0L
                val visitors = response.statsVisitorsData().firstOrNull()?.visitors?.toLong() ?: 0L
                val likes = response.statsLikesData().firstOrNull()?.likes?.toLong() ?: 0L
                val comments = response.statsCommentsData().firstOrNull()?.comments?.toLong() ?: 0L

                val aggregates = TodayAggregates(
                    views = views,
                    visitors = visitors,
                    likes = likes,
                    comments = comments
                )
                TodayAggregatesResult.Success(aggregates)
            }

            is WpRequestResult.WpError -> {
                appLogWrapper.e(AppLog.T.STATS, "API Error fetching today aggregates: ${result.errorMessage}")
                TodayAggregatesResult.Error(result.errorMessage)
            }

            else -> {
                appLogWrapper.e(AppLog.T.STATS, "Unknown error fetching today aggregates")
                TodayAggregatesResult.Error("Unknown error")
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
        if (accessToken == null) {
            appLogWrapper.e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
            return@withContext HourlyViewsResult.Error("Repository not initialized")
        }

        val calendar = Calendar.getInstance()
        // The API's endDate is exclusive for hourly queries, so we need to add 1 day to get
        // the target day's hours. Formula: 1 (for exclusive end) - offsetDays (0=today, 1=yesterday)
        // Examples: offsetDays=0 → tomorrow's date → fetches today's hours
        //           offsetDays=1 → today's date → fetches yesterday's hours
        calendar.add(Calendar.DAY_OF_YEAR, 1 - offsetDays)
        val dateString = getDateFormat().format(calendar.time)

        val params = StatsVisitsParams(
            unit = StatsVisitsUnit.HOUR,
            quantity = HOURLY_QUANTITY,
            endDate = dateString,
        )

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        when (result) {
            is WpRequestResult.Success -> {
                val response = result.response.data
                val dataPoints = response.statsVisitsData().map { dataPoint ->
                    HourlyViewsDataPoint(period = dataPoint.period, views = dataPoint.visits.toLong())
                }
                HourlyViewsResult.Success(dataPoints)
            }

            is WpRequestResult.WpError -> {
                appLogWrapper.e(AppLog.T.STATS, "API Error fetching hourly views: ${result.errorMessage}")
                HourlyViewsResult.Error(result.errorMessage)
            }

            else -> {
                appLogWrapper.e(AppLog.T.STATS, "Unknown error fetching hourly views")
                HourlyViewsResult.Error("Unknown error")
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
            if (accessToken == null) {
                appLogWrapper.e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
                return@withContext WeeklyStatsResult.Error("Repository not initialized")
            }

            val (startDate, endDate) = calculateWeekDateRange(weeksAgo)
            val endDateString = getDateFormat().format(endDate.time)

            val params = StatsVisitsParams(
                unit = StatsVisitsUnit.DAY,
                quantity = WEEKLY_QUANTITY,
                endDate = endDateString,
            )

            val result = wpComApiClient.request { requestBuilder ->
                requestBuilder.statsVisits().getStatsVisits(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
            }

            when (result) {
                is WpRequestResult.Success -> {
                    val response = result.response.data
                    val visitsData = response.statsVisitsData()
                    val visitorsData = response.statsVisitorsData()
                    val likesData = response.statsLikesData()
                    val commentsData = response.statsCommentsData()
                    val postsData = response.statsPostsData()

                    val totalViews = visitsData.sumOf { it.visits.toLong() }
                    val totalVisitors = visitorsData.sumOf { it.visitors.toLong() }
                    val totalLikes = likesData.sumOf { it.likes.toLong() }
                    val totalComments = commentsData.sumOf { it.comments.toLong() }
                    val totalPosts = postsData.sumOf { it.posts.toLong() }

                    val startDateFormatted = getDateFormat().format(startDate.time)

                    val aggregates = WeeklyAggregates(
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

                is WpRequestResult.WpError -> {
                    appLogWrapper.e(AppLog.T.STATS, "API Error fetching weekly stats: ${result.errorMessage}")
                    WeeklyStatsResult.Error(result.errorMessage)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.STATS, "Unknown error fetching weekly stats")
                    WeeklyStatsResult.Error("Unknown error")
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
            if (accessToken == null) {
                appLogWrapper.e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
                return@withContext DailyViewsResult.Error("Repository not initialized")
            }

            val (_, endDate) = calculateWeekDateRange(weeksAgo)
            val endDateString = getDateFormat().format(endDate.time)

            val params = StatsVisitsParams(
                unit = StatsVisitsUnit.DAY,
                quantity = WEEKLY_QUANTITY,
                endDate = endDateString,
            )

            val result = wpComApiClient.request { requestBuilder ->
                requestBuilder.statsVisits().getStatsVisits(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
            }

            when (result) {
                is WpRequestResult.Success -> {
                    val response = result.response.data
                    val dataPoints = response.statsVisitsData().map { dataPoint ->
                        DailyViewsDataPoint(period = dataPoint.period, views = dataPoint.visits.toLong())
                    }
                    DailyViewsResult.Success(dataPoints)
                }

                is WpRequestResult.WpError -> {
                    appLogWrapper.e(AppLog.T.STATS, "API Error fetching daily views: ${result.errorMessage}")
                    DailyViewsResult.Error(result.errorMessage)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.STATS, "Unknown error fetching daily views")
                    DailyViewsResult.Error("Unknown error")
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
        if (accessToken == null) {
            appLogWrapper.e(AppLog.T.STATS, "Cannot fetch stats: repository not initialized")
            return@withContext WeeklyStatsWithDailyDataResult.Error("Repository not initialized")
        }

        val (startDate, endDate) = calculateWeekDateRange(weeksAgo)
        val endDateString = getDateFormat().format(endDate.time)

        val params = StatsVisitsParams(
            unit = StatsVisitsUnit.DAY,
            quantity = WEEKLY_QUANTITY,
            endDate = endDateString,
        )

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        when (result) {
            is WpRequestResult.Success -> {
                val response = result.response.data
                val visitsData = response.statsVisitsData()
                val visitorsData = response.statsVisitorsData()
                val likesData = response.statsLikesData()
                val commentsData = response.statsCommentsData()
                val postsData = response.statsPostsData()

                // Build aggregates
                val totalViews = visitsData.sumOf { it.visits.toLong() }
                val totalVisitors = visitorsData.sumOf { it.visitors.toLong() }
                val totalLikes = likesData.sumOf { it.likes.toLong() }
                val totalComments = commentsData.sumOf { it.comments.toLong() }
                val totalPosts = postsData.sumOf { it.posts.toLong() }
                val startDateFormatted = getDateFormat().format(startDate.time)

                val aggregates = WeeklyAggregates(
                    views = totalViews,
                    visitors = totalVisitors,
                    likes = totalLikes,
                    comments = totalComments,
                    posts = totalPosts,
                    startDate = startDateFormatted,
                    endDate = endDateString
                )

                // Build daily data points
                val dailyDataPoints = visitsData.map { dataPoint ->
                    DailyViewsDataPoint(period = dataPoint.period, views = dataPoint.visits.toLong())
                }

                WeeklyStatsWithDailyDataResult.Success(aggregates, dailyDataPoints)
            }

            is WpRequestResult.WpError -> {
                appLogWrapper.e(
                    AppLog.T.STATS,
                    "API Error fetching weekly stats with daily data: ${result.errorMessage}"
                )
                WeeklyStatsWithDailyDataResult.Error(result.errorMessage)
            }

            else -> {
                appLogWrapper.e(AppLog.T.STATS, "Unknown error fetching weekly stats with daily data")
                WeeklyStatsWithDailyDataResult.Error("Unknown error")
            }
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
    data class Success(val aggregates: WeeklyAggregates) : WeeklyStatsResult()
    data class Error(val message: String) : WeeklyStatsResult()
}

/**
 * Weekly aggregated stats data.
 */
data class WeeklyAggregates(
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
    data class Success(val dataPoints: List<DailyViewsDataPoint>) : DailyViewsResult()
    data class Error(val message: String) : DailyViewsResult()
}

/**
 * Raw daily data point from the stats API.
 */
data class DailyViewsDataPoint(
    val period: String,
    val views: Long
)

/**
 * Result wrapper for combined weekly stats fetch operation.
 * Contains both aggregated stats and daily data points from a single API call.
 */
sealed class WeeklyStatsWithDailyDataResult {
    data class Success(
        val aggregates: WeeklyAggregates,
        val dailyDataPoints: List<DailyViewsDataPoint>
    ) : WeeklyStatsWithDailyDataResult()
    data class Error(val message: String) : WeeklyStatsWithDailyDataResult()
}
