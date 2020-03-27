package org.wordpress.android.fluxc.store.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.ReferrersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferrersStore
@Inject constructor(
    private val restClient: ReferrersRestClient,
    private val sqlUtils: ReferrersSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchReferrers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchReferrers") {
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, date, limitMode.limit)) {
            return@withDefaultContext OnStatsFetched(getReferrers(site, granularity, limitMode, date), cached = true)
        }
        val payload = restClient.fetchReferrers(site, granularity, date, limitMode.limit + 1, forced)
        return@withDefaultContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date, limitMode.limit)
                OnStatsFetched(timeStatsMapper.map(payload.response, limitMode))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getReferrers(site: SiteModel, granularity: StatsGranularity, limitMode: Top, date: Date) =
            coroutineEngine.run(STATS, this, "getReferrers") {
                sqlUtils.select(site, granularity, date)?.let { timeStatsMapper.map(it, limitMode) }
            }
}
