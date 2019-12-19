package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TodayInsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class TodayInsightsStore @Inject constructor(
    private val restClient: TodayInsightsRestClient,
    private val sqlUtils: TodayInsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchTodayInsights(siteModel: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        if (!forced && sqlUtils.hasFreshRequest(siteModel)) {
            return@withContext OnStatsFetched(getTodayInsights(siteModel), cached = true)
        }
        val response = restClient.fetchTimePeriodStats(siteModel, DAYS, forced)
        return@withContext when {
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

    fun getTodayInsights(site: SiteModel): VisitsModel? {
        return sqlUtils.select(site)?.let { insightsMapper.map(it) }
    }
}
