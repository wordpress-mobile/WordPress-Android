package org.wordpress.android.fluxc.store.stats

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.PostDetailStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.DetailedPostStatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostDetailStore
@Inject constructor(
    private val restClient: LatestPostInsightsRestClient,
    private val sqlUtils: DetailedPostStatsSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: PostDetailStatsMapper
) {
    suspend fun fetchPostDetail(
        site: SiteModel,
        postId: Long,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "fetchPostDetail") {
        if (!forced && sqlUtils.hasFreshRequest(site, postId = postId)) {
            return@withDefaultContext OnStatsFetched(getPostDetail(site, postId), cached = true)
        }
        val payload = restClient.fetchPostStats(site, postId, forced)
        return@withDefaultContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, postId = postId)
                OnStatsFetched(mapper.map(payload.response))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getPostDetail(site: SiteModel, postId: Long) = coroutineEngine.run(AppLog.T.STATS, this, "getPostDetail") {
        sqlUtils.select(site, postId)?.let { mapper.map(it) }
    }
}
