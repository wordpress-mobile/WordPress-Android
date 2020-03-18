package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TagsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagsStore @Inject constructor(
    private val restClient: TagsRestClient,
    private val sqlUtils: TagsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchTags(siteModel: SiteModel, limitMode: Top, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchTags") {
                if (!forced && sqlUtils.hasFreshRequest(siteModel, limitMode.limit)) {
                    return@withDefaultContext OnStatsFetched(getTags(siteModel, limitMode), cached = true)
                }
                val response = restClient.fetchTags(siteModel, max = limitMode.limit + 1, forced = forced)
                return@withDefaultContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response, requestedItems = limitMode.limit)
                        OnStatsFetched(
                                insightsMapper.map(response.response, limitMode)
                        )
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getTags(site: SiteModel, cacheMode: LimitMode) = coroutineEngine.run(STATS, this, "getTags") {
        sqlUtils.select(site)?.let { insightsMapper.map(it, cacheMode) }
    }
}
