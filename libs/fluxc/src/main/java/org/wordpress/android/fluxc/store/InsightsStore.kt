package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

@Singleton
class InsightsStore
@Inject constructor(
    private val restClient: InsightsRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val coroutineContext: CoroutineContext
) {
    // All time insights
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val payload = restClient.fetchAllTimeInsights(site, forced)
        return@withContext when {
            payload.isError -> OnInsightsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response)
                OnInsightsFetched(payload.response.toDomainModel(site))
            }
            else -> OnInsightsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getAllTimeInsights(site: SiteModel): InsightsAllTimeModel? {
        return sqlUtils.selectAllTimeInsights(site)?.toDomainModel(site)
    }

    private fun AllTimeResponse.toDomainModel(site: SiteModel): InsightsAllTimeModel {
        val stats = this.stats
        return InsightsAllTimeModel(
                site.siteId,
                this.date,
                stats.visitors,
                stats.views,
                stats.posts,
                stats.viewsBestDay,
                stats.viewsBestDayTotal
        )
    }

    // Most popular insights
    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val payload = restClient.fetchMostPopularInsights(site, forced)
        return@withContext when {
            payload.isError -> OnInsightsFetched(payload.error)
            payload.response != null -> {
                val data = payload.response
                sqlUtils.insert(site, data)
                OnInsightsFetched(
                        data.toDomainModel(site)
                )
            }
            else -> OnInsightsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getMostPopularInsights(site: SiteModel): InsightsMostPopularModel? {
        return sqlUtils.selectMostPopularInsights(site)?.toDomainModel(site)
    }

    private fun MostPopularResponse.toDomainModel(site: SiteModel): InsightsMostPopularModel {
        return InsightsMostPopularModel(
                site.siteId,
                this.highestDayOfWeek,
                this.highestHour,
                this.highestDayPercent,
                this.highestHourPercent
        )
    }

    // Latest post insights
    suspend fun fetchLatestPostInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val latestPost = restClient.fetchLatestPostForInsights(site, forced)
        val postsFound = latestPost.response?.postsFound

        val posts = latestPost.response?.posts
        return@withContext if (postsFound != null && postsFound > 0 && posts != null && posts.isNotEmpty()) {
            val latestPost = posts[0]
            val postStats = restClient.fetchPostStats(site, latestPost.id, forced)
            when {
                postStats.response != null -> {
                    sqlUtils.insert(site, latestPost)
                    sqlUtils.insert(site, postStats.response)
                    OnInsightsFetched((latestPost to postStats.response).toDomainModel(site))
                }
                postStats.isError -> OnInsightsFetched(postStats.error)
                else -> OnInsightsFetched()
            }
        } else if (latestPost.isError) {
            OnInsightsFetched(latestPost.error)
        } else {
            OnInsightsFetched()
        }
    }

    fun getLatestPostInsights(site: SiteModel): InsightsLatestPostModel? {
        val latestPostDetailResponse = sqlUtils.selectLatestPostDetail(site)
        val latestPostViewsResponse = sqlUtils.selectLatestPostStats(site)
        return if (latestPostDetailResponse != null && latestPostViewsResponse != null) {
            (latestPostDetailResponse to latestPostViewsResponse).toDomainModel(site)
        } else {
            null
        }
    }

    private fun Pair<PostResponse, PostStatsResponse>.toDomainModel(site: SiteModel): InsightsLatestPostModel {
        val daysViews = if (second.fields.size > 1 && second.fields[0] == "period" && second.fields[1] == "views") {
            second.data.map { list -> list[0] to list[1].toInt() }
        } else {
            listOf()
        }
        val viewsCount = second.views
        val commentCount = first.discussion?.commentCount ?: 0
        return InsightsLatestPostModel(
                site.siteId,
                first.title,
                first.url,
                first.date,
                first.id,
                viewsCount,
                commentCount,
                first.likeCount,
                daysViews
        )
    }

    data class OnInsightsFetched<T>(val model: T? = null) : Store.OnChanged<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class FetchInsightsPayload<T>(
        val response: T? = null
    ) : Payload<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    enum class StatsErrorType {
        GENERIC_ERROR,
        TIMEOUT,
        API_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE
    }

    class StatsError(var type: StatsErrorType, var message: String? = null) : Store.OnChangedError
}
