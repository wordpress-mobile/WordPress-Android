package org.wordpress.android.fluxc.store.stats.insights

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.DetailedPostStatsSqlUtils
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.LatestPostDetailSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatestPostInsightsStore @Inject constructor(
    private val restClient: LatestPostInsightsRestClient,
    private val latestPostDetailSqlUtils: LatestPostDetailSqlUtils,
    private val detailedPostStats: DetailedPostStatsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchLatestPostInsights(site: SiteModel, forced: Boolean = false) =
            coroutineEngine.withDefaultContext(STATS, this, "fetchLatestPostInsights") {
                if (!forced && latestPostDetailSqlUtils.hasFreshRequest(site)) {
                    return@withDefaultContext OnStatsFetched(getLatestPostInsights(site), cached = true)
                }
                val latestPostPayload = restClient.fetchLatestPostForInsights(site, forced)
                val postsFound = latestPostPayload.response?.postsFound

                val posts = latestPostPayload.response?.posts
                return@withDefaultContext if (postsFound != null &&
                        postsFound > 0 &&
                        posts != null &&
                        posts.isNotEmpty()) {
                    val latestPost = posts[0]
                    val postStats = restClient.fetchPostStats(site, latestPost.id, forced)
                    when {
                        postStats.response != null -> {
                            latestPostDetailSqlUtils.insert(site, latestPost)
                            detailedPostStats.insert(site, postStats.response, postId = latestPost.id)
                            OnStatsFetched(insightsMapper.map(latestPost, postStats.response, site))
                        }
                        postStats.isError -> OnStatsFetched(postStats.error)
                        else -> OnStatsFetched()
                    }
                } else if (latestPostPayload.isError) {
                    OnStatsFetched(latestPostPayload.error)
                } else {
                    OnStatsFetched()
                }
            }

    fun getLatestPostInsights(site: SiteModel) = coroutineEngine.run(
            STATS, this, "getLatestPostInsights"
    ) {
        latestPostDetailSqlUtils.select(site)?.let { latestPostDetailResponse ->
            detailedPostStats.select(site, latestPostDetailResponse.id)?.let { latestPostViewsResponse ->
                insightsMapper.map(latestPostDetailResponse, latestPostViewsResponse, site)
            }
        }
    }
}
