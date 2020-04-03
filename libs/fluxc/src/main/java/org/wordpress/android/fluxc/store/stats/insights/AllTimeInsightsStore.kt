package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.AllTimeSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllTimeInsightsStore @Inject constructor(
    private val restClient: AllTimeInsightsRestClient,
    private val sqlUtils: AllTimeSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "fetchAllTimeInsights") {
                if (!forced && sqlUtils.hasFreshRequest(site)) {
                    return@withDefaultContext OnStatsFetched(getAllTimeInsights(site), cached = true)
                }
                val payload = restClient.fetchAllTimeInsights(site, forced)
                return@withDefaultContext when {
                    payload.isError -> OnStatsFetched(payload.error)
                    payload.response != null -> {
                        sqlUtils.insert(site, payload.response)
                        OnStatsFetched(insightsMapper.map(payload.response, site))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getAllTimeInsights(site: SiteModel): InsightsAllTimeModel? =
            coroutineEngine.run(AppLog.T.STATS, this, "getAllTimeInsights") {
                sqlUtils.select(site)?.let { insightsMapper.map(it, site) }
            }
}
