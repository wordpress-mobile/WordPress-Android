package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.MostPopularSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class MostPopularInsightsStore @Inject constructor(
    private val restClient: MostPopularRestClient,
    private val sqlUtils: MostPopularSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        if (!forced && sqlUtils.hasFreshRequest(site)) {
            return@withContext OnStatsFetched(getMostPopularInsights(site), cached = true)
        }
        val payload = restClient.fetchMostPopularInsights(site, forced)
        return@withContext when {
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
            withContext(coroutineContext) {
                if (!forced && sqlUtils.hasFreshRequest(site)) {
                    return@withContext OnStatsFetched(getYearsInsights(site), cached = true)
                }
                val payload = restClient.fetchMostPopularInsights(site, forced)
                return@withContext when {
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

    fun getMostPopularInsights(site: SiteModel): InsightsMostPopularModel? {
        return sqlUtils.select(site)?.let { insightsMapper.map(it, site) }
    }

    fun getYearsInsights(site: SiteModel): YearsInsightsModel? {
        return sqlUtils.select(site)?.let { insightsMapper.map(it) }
    }
}
