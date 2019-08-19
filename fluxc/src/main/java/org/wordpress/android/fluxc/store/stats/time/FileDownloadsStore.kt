package org.wordpress.android.fluxc.store.stats.time

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.FileDownloadsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.FileDownloadsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.FileDownloadsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class FileDownloadsStore
@Inject constructor(
    private val restClient: FileDownloadsRestClient,
    private val sqlUtils: FileDownloadsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchFileDownloads(
        site: SiteModel,
        period: StatsGranularity,
        limitMode: Top,
        date: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        if (!forced && sqlUtils.hasFreshRequest(site, period, date, limitMode.limit)) {
            return@withContext OnStatsFetched(getFileDownloads(site, period, limitMode, date), cached = true)
        }
        val payload = restClient.fetchFileDownloads(site, period, date, limitMode.limit + 1, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, period, date, limitMode.limit)
                OnStatsFetched(timeStatsMapper.map(payload.response, limitMode))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getFileDownloads(
        site: SiteModel,
        period: StatsGranularity,
        limitMode: LimitMode,
        date: Date
    ): FileDownloadsModel? {
        return sqlUtils.select(site, period, date)?.let { timeStatsMapper.map(it, limitMode) }
    }
}
