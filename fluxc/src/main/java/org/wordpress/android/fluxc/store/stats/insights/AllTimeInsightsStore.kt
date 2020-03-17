package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.AllTimeSqlUtils
import org.wordpress.android.fluxc.store.BaseStore
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class AllTimeInsightsStore @Inject constructor(
    private val restClient: AllTimeInsightsRestClient,
    private val sqlUtils: AllTimeSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext,
    loggerFactory: LoggerFactory
) : BaseStore(loggerFactory.build(AppLog.T.STATS)) {
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean = false) =
            withContext(coroutineContext, "fetchAllTimeInsights") {
                if (!forced && sqlUtils.hasFreshRequest(site)) {
                    return@withContext OnStatsFetched(getAllTimeInsights(site), cached = true)
                }
                val payload = restClient.fetchAllTimeInsights(site, forced)
                return@withContext when {
                    payload.isError -> OnStatsFetched(payload.error)
                    payload.response != null -> {
                        sqlUtils.insert(site, payload.response)
                        OnStatsFetched(insightsMapper.map(payload.response, site))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getAllTimeInsights(site: SiteModel): InsightsAllTimeModel? = withLog("getAllTimeInsights") {
        return@withLog sqlUtils.select(site)?.let { insightsMapper.map(it, site) }
    }
}
