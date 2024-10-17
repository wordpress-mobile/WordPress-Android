package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TodayInsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayInsightsStore @Inject constructor(
    private val restClient: TodayInsightsRestClient,
    private val sqlUtils: TodayInsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchTodayInsights(siteModel: SiteModel, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchTodayInsights") {
                if (!forced && sqlUtils.hasFreshRequest(siteModel)) {
                    return@withDefaultContext OnStatsFetched(getTodayInsights(siteModel), cached = true)
                }
                val response = restClient.fetchTimePeriodStats(siteModel, DAYS, forced)
                return@withDefaultContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(insightsMapper.map(response.response))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getTodayInsights(site: SiteModel) = coroutineEngine.run(STATS, this, "getTodayInsights") {
        sqlUtils.select(site)?.let { insightsMapper.map(it) }
    }
}
