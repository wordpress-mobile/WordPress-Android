package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class LatestPostInsightsStore @Inject constructor(
    private val restClient: LatestPostInsightsRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchLatestPostInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val latestPostPayload = restClient.fetchLatestPostForInsights(site, forced)
        val postsFound = latestPostPayload.response?.postsFound

        val posts = latestPostPayload.response?.posts
        return@withContext if (postsFound != null && postsFound > 0 && posts != null && posts.isNotEmpty()) {
            val latestPost = posts[0]
            val postStats = restClient.fetchPostStats(site, latestPost.id, forced)
            when {
                postStats.response != null -> {
                    sqlUtils.insert(site, latestPost)
                    sqlUtils.insert(site, latestPost.id, postStats.response)
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

    fun getLatestPostInsights(site: SiteModel): InsightsLatestPostModel? {
        return sqlUtils.selectLatestPostDetail(site)?.let { latestPostDetailResponse ->
            sqlUtils.selectDetailedPostStats(site, latestPostDetailResponse.id)?.let { latestPostViewsResponse ->
                insightsMapper.map(latestPostDetailResponse, latestPostViewsResponse, site)
            }
        }
    }
}
