package org.wordpress.android.fluxc.store.stats.time

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class AuthorsStore
@Inject constructor(
    private val restClient: AuthorsRestClient,
    private val sqlUtils: TimeStatsSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchAuthors(
        site: SiteModel,
        pageSize: Int,
        period: StatsGranularity,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        val payload = restClient.fetchAuthors(site, period, pageSize + 1, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, period)
                OnStatsFetched(timeStatsMapper.map(payload.response, pageSize))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getAuthors(site: SiteModel, period: StatsGranularity, pageSize: Int): AuthorsModel? {
        return sqlUtils.selectAuthors(site, period)?.let { timeStatsMapper.map(it, pageSize) }
    }
}
