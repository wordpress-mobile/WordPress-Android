package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.PublicizeSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicizeStore
@Inject constructor(
    private val restClient: PublicizeRestClient,
    private val sqlUtils: PublicizeSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchPublicizeData(siteModel: SiteModel, limitMode: LimitMode, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchPublicizeData") {
                if (!forced && sqlUtils.hasFreshRequest(siteModel)) {
                    return@withDefaultContext OnStatsFetched(getPublicizeData(siteModel, limitMode), cached = true)
                }
                val response = restClient.fetchPublicizeData(siteModel, forced = forced)
                return@withDefaultContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(insightsMapper.map(response.response, limitMode))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getPublicizeData(site: SiteModel, limitMode: LimitMode) = coroutineEngine.run(STATS, this, "getPublicizeData") {
        sqlUtils.select(site)?.let { insightsMapper.map(it, limitMode) }
    }
}
