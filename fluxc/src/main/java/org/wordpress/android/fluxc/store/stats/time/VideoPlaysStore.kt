package org.wordpress.android.fluxc.store.stats.time

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.model.stats.time.VideoPlaysModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class VideoPlaysStore
@Inject constructor(
    private val restClient: VideoPlaysRestClient,
    private val sqlUtils: TimeStatsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchVideoPlays(
        site: SiteModel,
        pageSize: Int,
        granularity: StatsGranularity,
        date: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        val payload = restClient.fetchVideoPlays(site, granularity, date, pageSize + 1, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date)
                OnStatsFetched(timeStatsMapper.map(payload.response, pageSize))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getVideoPlays(site: SiteModel, period: StatsGranularity, pageSize: Int, date: Date): VideoPlaysModel? {
        return sqlUtils.selectVideoPlays(site, period, date)?.let { timeStatsMapper.map(it, pageSize) }
    }
}
