package org.wordpress.android.fluxc.store.stats.time

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VideoPlaysModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.VideoPlaysSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class VideoPlaysStore
@Inject constructor(
    private val restClient: VideoPlaysRestClient,
    private val sqlUtils: VideoPlaysSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchVideoPlays(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: LimitMode.Top,
        date: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, date, limitMode.limit)) {
            return@withContext OnStatsFetched(getVideoPlays(site, granularity, limitMode, date), cached = true)
        }
        val payload = restClient.fetchVideoPlays(site, granularity, date, limitMode.limit + 1, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date, limitMode.limit)
                OnStatsFetched(timeStatsMapper.map(payload.response, limitMode))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getVideoPlays(site: SiteModel, period: StatsGranularity, limitMode: LimitMode, date: Date): VideoPlaysModel? {
        return sqlUtils.select(site, period, date)?.let { timeStatsMapper.map(it, limitMode) }
    }
}
