package org.wordpress.android.ui.newstats.todaysstat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsVisitsDataValue
import uniffi.wp_api.StatsVisitsParams
import uniffi.wp_api.StatsVisitsUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

private const val HOURLY_QUANTITY = 24u
private const val DAILY_QUANTITY = 1u

// Daily aggregates response field indexes
// Response fields order: period, views, visitors, likes, reblogs, comments, posts
@Suppress("unused") private const val INDEX_PERIOD = 0
private const val INDEX_VIEWS = 1
private const val INDEX_VISITORS = 2
private const val INDEX_LIKES = 3
@Suppress("unused") private const val INDEX_REBLOGS = 4
private const val INDEX_COMMENTS = 5
@Suppress("unused") private const val INDEX_POSTS = 6

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

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

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
        val dateString = dateFormat.format(calendar.time)

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
                val row = response.data.firstOrNull()
                val aggregates = row?.let { parseDailyAggregates(it) }
                if (aggregates != null) {
                    TodayAggregatesResult.Success(aggregates)
                } else {
                    TodayAggregatesResult.Error("No data available")
                }
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
        val dateString = dateFormat.format(calendar.time)

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
                val dataPoints = response.data.mapNotNull { row ->
                    parseHourlyDataRow(row)
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

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private fun parseHourlyDataRow(row: Any?): HourlyViewsDataPoint? {
        return try {
            val rowList = row as? List<*> ?: return null
            val periodValue = rowList.getOrNull(0)
            val viewsValue = rowList.getOrNull(1)

            // Extract values from wrapper types
            val period = when (periodValue) {
                is StatsVisitsDataValue.String -> periodValue.v1
                else -> return null
            }

            val views = when (viewsValue) {
                is StatsVisitsDataValue.Number -> viewsValue.v1.toLong()
                else -> 0L
            }

            HourlyViewsDataPoint(period = period, views = views)
        } catch (e: Exception) {
            appLogWrapper.w(AppLog.T.STATS, "Failed to parse stats row: ${e.message}")
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun parseDailyAggregates(row: Any?): TodayAggregates? {
        return try {
            val rowList = row as? List<*> ?: return null
            val viewsValue = rowList.getOrNull(INDEX_VIEWS)
            val visitorsValue = rowList.getOrNull(INDEX_VISITORS)
            val likesValue = rowList.getOrNull(INDEX_LIKES)
            val commentsValue = rowList.getOrNull(INDEX_COMMENTS)

            TodayAggregates(
                views = extractLongValue(viewsValue),
                visitors = extractLongValue(visitorsValue),
                likes = extractLongValue(likesValue),
                comments = extractLongValue(commentsValue)
            )
        } catch (e: Exception) {
            appLogWrapper.w(AppLog.T.STATS, "Failed to parse daily aggregates: ${e.message}")
            null
        }
    }

    private fun extractLongValue(value: Any?): Long {
        return when (value) {
            is StatsVisitsDataValue.Number -> value.v1.toLong()
            else -> 0L
        }
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
