package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.SummaryRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.SummarySqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryStore @Inject constructor(
    private val restClient: SummaryRestClient,
    private val sqlUtils: SummarySqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchSummary(site: SiteModel, forced: Boolean = false) =
        coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "fetchSummary") {
            if (!forced && sqlUtils.hasFreshRequest(site)) {
                return@withDefaultContext OnStatsFetched(getSummary(site), cached = true)
            }
            val payload = restClient.fetchSummary(site, forced)
            return@withDefaultContext when {
                payload.isError -> OnStatsFetched(payload.error)
                payload.response != null -> {
                    sqlUtils.insert(site, payload.response)
                    OnStatsFetched(insightsMapper.map(payload.response))
                }
                else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
            }
        }

    fun getSummary(site: SiteModel) = coroutineEngine.run(AppLog.T.STATS, this, "getSummary") {
        sqlUtils.select(site)?.let { insightsMapper.map(it) }
    }
}
