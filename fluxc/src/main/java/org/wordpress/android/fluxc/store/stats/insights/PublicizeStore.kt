package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.PublicizeSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class PublicizeStore
@Inject constructor(
    private val restClient: PublicizeRestClient,
    private val sqlUtils: PublicizeSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchPublicizeData(siteModel: SiteModel, pageSize: Int, forced: Boolean = false) =
            withContext(coroutineContext) {
                val response = restClient.fetchPublicizeData(siteModel, pageSize = pageSize + 1, forced = forced)
                return@withContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(insightsMapper.map(response.response, pageSize))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getPublicizeData(site: SiteModel, pageSize: Int): PublicizeModel? {
        return sqlUtils.select(site)?.let { insightsMapper.map(it, pageSize) }
    }
}
