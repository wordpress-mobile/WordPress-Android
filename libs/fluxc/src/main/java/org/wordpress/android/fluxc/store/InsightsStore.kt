package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.StatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.StatsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.StatsRestClient.PostViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.StatsRestClient.PostsResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class InsightsStore
@Inject constructor(private val statsRestClient: StatsRestClient, private val coroutineContext: CoroutineContext) {
    suspend fun fetchAllTimeInsights(site: SiteModel) = withContext(coroutineContext) {
        val payload = statsRestClient.fetchAllTimeInsights(site)
        if (payload.isError) {
            return@withContext OnAllTimeInsightsFetched(payload.error)
        } else {
            val data = payload.model
            val stats = data!!.stats
            return@withContext OnAllTimeInsightsFetched(
                    InsightsAllTimeModel(
                            site.siteId,
                            data.date,
                            stats.visitors,
                            stats.views,
                            stats.posts,
                            stats.viewsBestDay,
                            stats.viewsBestDayTotal
                    )
            )
        }
    }

    suspend fun fetchLatestPostInsights(site: SiteModel): OnLatestPostInsightsFetched {
        val responsePost = statsRestClient.fetchLatestPostForInsights(site)
        val postsFound = responsePost.response?.postsFound

        val posts = responsePost.response?.posts
        if (postsFound != null && postsFound > 0 && posts != null && posts.isNotEmpty()) {
            val latestPost = posts[0]
            val postViews = statsRestClient.fetchPostViewsForInsights(site, latestPost.id)
            val commentCount = latestPost.discussion?.commentCount ?: 0
            val viewsCount = postViews.response?.views ?: 0
            return OnLatestPostInsightsFetched(
                    InsightsLatestPostModel(
                            site.siteId,
                            latestPost.title,
                            latestPost.url,
                            latestPost.date,
                            latestPost.id,
                            viewsCount,
                            commentCount,
                            latestPost.likeCount
                    )
            )
        } else if (responsePost.isError) {
            return OnLatestPostInsightsFetched(responsePost.error)
        } else {
            return OnLatestPostInsightsFetched()
        }
    }

    data class OnAllTimeInsightsFetched(
        val allTimeModel: InsightsAllTimeModel? = null
    ) : Store.OnChanged<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class OnLatestPostInsightsFetched(
        val latestPostModel: InsightsLatestPostModel? = null
    ) : Store.OnChanged<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class FetchAllTimeInsightsPayload(
        val model: AllTimeResponse? = null
    ) : Payload<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class FetchLatestPostPayload(
        val response: PostsResponse? = null
    ) : Payload<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class FetchLatestPostViewsPayload(
        val response: PostViewsResponse? = null
    ) : Payload<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    enum class StatsErrorType {
        GENERIC_ERROR,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class StatsError(var type: StatsErrorType, var message: String? = null) : Store.OnChangedError
}
