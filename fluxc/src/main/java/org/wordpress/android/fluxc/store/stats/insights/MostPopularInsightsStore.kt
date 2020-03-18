package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.MostPopularSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MostPopularInsightsStore @Inject constructor(
    private val restClient: MostPopularRestClient,
    private val sqlUtils: MostPopularSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchMostPopularInsights") {
                if (!forced && sqlUtils.hasFreshRequest(site)) {
                    return@withDefaultContext OnStatsFetched(getMostPopularInsights(site), cached = true)
                }
                val payload = restClient.fetchMostPopularInsights(site, forced)
                return@withDefaultContext when {
                    payload.isError -> OnStatsFetched(payload.error)
                    payload.response != null -> {
                        val data = payload.response
                        sqlUtils.insert(site, data)
                        OnStatsFetched(
                                insightsMapper.map(data, site)
                        )
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    suspend fun fetchYearsInsights(site: SiteModel, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchYearsInsights") {
                if (!forced && sqlUtils.hasFreshRequest(site)) {
                    return@withDefaultContext OnStatsFetched(getYearsInsights(site), cached = true)
                }
                val payload = restClient.fetchMostPopularInsights(site, forced)
                return@withDefaultContext when {
                    payload.isError -> OnStatsFetched(payload.error)
                    payload.response != null -> {
                        val data = payload.response
                        sqlUtils.insert(site, data)
                        OnStatsFetched(
                                insightsMapper.map(data)
                        )
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getMostPopularInsights(site: SiteModel) = coroutineEngine.run(STATS, this, "getMostPopularInsights") {
        sqlUtils.select(site)?.let { insightsMapper.map(it, site) }
    }

    fun getYearsInsights(site: SiteModel) = coroutineEngine.run(STATS, this, "getYearsInsights") {
        sqlUtils.select(site)?.let { insightsMapper.map(it) }
    }
}
