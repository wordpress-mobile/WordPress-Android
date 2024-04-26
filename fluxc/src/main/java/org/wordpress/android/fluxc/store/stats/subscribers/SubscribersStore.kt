package org.wordpress.android.fluxc.store.stats.subscribers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersMapper
import org.wordpress.android.fluxc.model.stats.subscribers.SubscribersModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.SubscribersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.SubscribersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscribersStore @Inject constructor(
    private val restClient: SubscribersRestClient,
    private val sqlUtils: SubscribersSqlUtils,
    private val subscribersMapper: SubscribersMapper,
    private val statsUtils: StatsUtils,
    private val currentTimeProvider: CurrentTimeProvider,
    private val coroutineEngine: CoroutineEngine,
    private val appLogWrapper: AppLogWrapper
) {
    suspend fun fetchSubscribers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchSubscribers") {
        val dateWithTimeZone = statsUtils.getFormattedDate(
            currentTimeProvider.currentDate(),
            SiteUtils.getNormalizedTimezone(site.timezone)
        )
        logProgress(granularity, "Site timezone: ${site.timezone}")
        logProgress(granularity, "Fetching for date with applied timezone: $dateWithTimeZone")
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, dateWithTimeZone, limitMode.limit)) {
            logProgress(granularity, "Loading cached data")
            return@withDefaultContext OnStatsFetched(
                getSubscribers(site, granularity, limitMode, dateWithTimeZone),
                cached = true
            )
        }
        val payload = restClient.fetchSubscribers(site, granularity, limitMode.limit, dateWithTimeZone, forced)
        return@withDefaultContext when {
            payload.isError -> {
                logProgress(granularity, "Error fetching data: ${payload.error}")
                OnStatsFetched(payload.error)
            }

            payload.response != null -> {
                logProgress(granularity, "Data fetched correctly")
                sqlUtils.insert(site, payload.response, granularity, dateWithTimeZone, limitMode.limit)
                val subscribersResponse = subscribersMapper.map(payload.response, limitMode)
                if (subscribersResponse.period.isBlank() || subscribersResponse.dates.isEmpty()) {
                    logProgress(granularity, "Invalid response")
                    OnStatsFetched(
                        StatsError(INVALID_RESPONSE, "Subscribers: Required data 'period' or 'dates' missing")
                    )
                } else {
                    logProgress(granularity, "Valid response returned for period: ${subscribersResponse.period}")
                    logProgress(granularity, "Last data item for: ${subscribersResponse.dates.lastOrNull()?.period}")
                    OnStatsFetched(subscribersResponse)
                }
            }

            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    private fun logProgress(granularity: StatsGranularity, message: String) {
        appLogWrapper.d(STATS, "fetchSubscribers for $granularity: $message")
    }

    fun getSubscribers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode
    ): SubscribersModel? {
        val dateWithTimeZone = statsUtils.getFormattedDate(
            currentTimeProvider.currentDate(),
            SiteUtils.getNormalizedTimezone(site.timezone)
        )
        return getSubscribers(site, granularity, limitMode, dateWithTimeZone)
    }

    fun getSubscribers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date
    ): SubscribersModel? {
        val dateWithTimeZone = statsUtils.getFormattedDate(date, SiteUtils.getNormalizedTimezone(site.timezone))
        return getSubscribers(site, granularity, limitMode, dateWithTimeZone)
    }

    private fun getSubscribers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode,
        dateWithTimeZone: String
    ) = coroutineEngine.run(STATS, this, "getSubscribers") {
        sqlUtils.select(site, granularity, dateWithTimeZone)?.let { subscribersMapper.map(it, limitMode) }
    }
}
