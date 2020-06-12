package org.wordpress.android.fluxc.store.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.VisitsAndViewsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitsAndViewsStore
@Inject constructor(
    private val restClient: VisitAndViewsRestClient,
    private val sqlUtils: VisitsAndViewsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val statsUtils: StatsUtils,
    private val currentTimeProvider: CurrentTimeProvider,
    private val coroutineEngine: CoroutineEngine,
    private val appLogWrapper: AppLogWrapper
) {
    suspend fun fetchVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode.Top,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchVisits") {
        val dateWithTimeZone = statsUtils.getFormattedDate(
                currentTimeProvider.currentDate,
                SiteUtils.getNormalizedTimezone(site.timezone)
        )
        logProgress(granularity, "Site timezone: ${site.timezone}")
        logProgress(granularity, "Current date: ${currentTimeProvider.currentDate}")
        logProgress(granularity, "Fetching for date with applied timezone: $dateWithTimeZone")
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, dateWithTimeZone, limitMode.limit)) {
            logProgress(granularity, "Loading cached data")
            return@withDefaultContext OnStatsFetched(
                    getVisits(site, granularity, limitMode, dateWithTimeZone),
                    cached = true
            )
        }
        val payload = restClient.fetchVisits(site, granularity, dateWithTimeZone, limitMode.limit, forced)
        return@withDefaultContext when {
            payload.isError -> {
                logProgress(granularity, "Error fetching data: ${payload.error}")
                OnStatsFetched(payload.error)
            }
            payload.response != null -> {
                logProgress(granularity, "Data fetched correctly")
                sqlUtils.insert(site, payload.response, granularity, dateWithTimeZone, limitMode.limit)
                val overviewResponse = timeStatsMapper.map(payload.response, limitMode)
                if (overviewResponse.period.isBlank() || overviewResponse.dates.isEmpty()) {
                    logProgress(granularity, "Invalid response")
                    OnStatsFetched(StatsError(INVALID_RESPONSE, "Overview: Required data 'period' or 'dates' missing"))
                } else {
                    logProgress(granularity, "Valid response returned for period: ${overviewResponse.period}")
                    logProgress(granularity, "Last data item for: ${overviewResponse.dates.lastOrNull()?.period}")
                    OnStatsFetched(overviewResponse)
                }
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    private fun logProgress(granularity: StatsGranularity, message: String) {
        appLogWrapper.d(STATS, "fetchVisits for $granularity: $message")
    }

    fun getVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode
    ): VisitsAndViewsModel? {
        val dateWithTimeZone = statsUtils.getFormattedDate(
                currentTimeProvider.currentDate,
                SiteUtils.getNormalizedTimezone(site.timezone)
        )
        return getVisits(site, granularity, limitMode, dateWithTimeZone)
    }

    private fun getVisits(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode,
        dateWithTimeZone: String
    ) = coroutineEngine.run(STATS, this, "getVisits") {
        sqlUtils.select(site, granularity, dateWithTimeZone)?.let { timeStatsMapper.map(it, limitMode) }
    }
}
