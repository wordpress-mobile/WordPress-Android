package org.wordpress.android.ui.newstats.todaysstat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.ui.newstats.extension.statsCommentsData
import org.wordpress.android.ui.newstats.extension.statsLikesData
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
