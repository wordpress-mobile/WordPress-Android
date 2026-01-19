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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

private const val HOURLY_UNIT = "hour"
private const val HOURLY_QUANTITY = 24u

/**
 * Repository for fetching stats data using the wordpress-rs API.
 * Handles hourly visits/views data for the Today's Stats card chart.
 */
class TodaysStatsRepository @Inject constructor(
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
        calendar.add(Calendar.DAY_OF_YEAR, 1 - offsetDays)
        val dateString = dateFormat.format(calendar.time)

        val params = StatsVisitsParams(
            unit = HOURLY_UNIT,
            quantity = HOURLY_QUANTITY,
            date = dateString,
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
                    parseDataRow(row)
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

    @Suppress("TooGenericExceptionCaught")
    private fun parseDataRow(row: Any?): HourlyViewsDataPoint? {
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
