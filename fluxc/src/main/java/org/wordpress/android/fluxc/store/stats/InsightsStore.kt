package org.wordpress.android.fluxc.store.stats

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CacheMode
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.FetchMode
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.model.stats.InsightsLatestPostModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.model.stats.FetchMode.Paged
import org.wordpress.android.fluxc.model.stats.PublicizeModel
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.model.stats.VisitsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class InsightsStore
@Inject constructor(
    private val restClient: InsightsRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val timeProvider: CurrentTimeProvider,
    private val coroutineContext: CoroutineContext
) {
    // All time insights
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val payload = restClient.fetchAllTimeInsights(site, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response)
                OnStatsFetched(insightsMapper.map(payload.response, site))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getAllTimeInsights(site: SiteModel): InsightsAllTimeModel? {
        return sqlUtils.selectAllTimeInsights(site)?.let { insightsMapper.map(it, site) }
    }

    // Most popular insights
    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
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

    fun getMostPopularInsights(site: SiteModel): InsightsMostPopularModel? {
        return sqlUtils.selectMostPopularInsights(site)?.let { insightsMapper.map(it, site) }
    }

    // Latest post insights
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
                    sqlUtils.insert(site, postStats.response)
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
        val latestPostDetailResponse = sqlUtils.selectLatestPostDetail(site)
        val latestPostViewsResponse = sqlUtils.selectLatestPostStats(site)
        return if (latestPostDetailResponse != null && latestPostViewsResponse != null) {
            insightsMapper.map(latestPostDetailResponse, latestPostViewsResponse, site)
        } else {
            null
        }
    }

    // Time period stats
    suspend fun fetchTodayInsights(siteModel: SiteModel, forced: Boolean = false) = withContext(coroutineContext) {
        val response = restClient.fetchTimePeriodStats(siteModel, DAYS, timeProvider.currentDate, forced)
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
        return sqlUtils.selectTodayInsights(site)?.let { insightsMapper.map(it) }
    }

    // Followers stats
    suspend fun fetchWpComFollowers(
        siteModel: SiteModel,
        fetchMode: Paged,
        forced: Boolean = false
    ): OnStatsFetched<FollowersModel> {
        return fetchFollowers(siteModel, forced, WP_COM, fetchMode)
    }

    suspend fun fetchEmailFollowers(
        siteModel: SiteModel,
        fetchMode: Paged,
        forced: Boolean = false
    ): OnStatsFetched<FollowersModel> {
        return fetchFollowers(siteModel, forced, EMAIL, fetchMode)
    }

    private suspend fun fetchFollowers(
        siteModel: SiteModel,
        forced: Boolean = false,
        followerType: FollowerType,
        fetchMode: Paged
    ) = withContext(coroutineContext) {
        val nextPage = if (fetchMode.loadMore) {
            val savedFollowers = sqlUtils.selectAllFollowers(siteModel, followerType).sumBy { it.subscribers.size }
            savedFollowers / fetchMode.pageSize + 1
        } else {
            1
        }

        val responsePayload = restClient.fetchFollowers(siteModel, followerType, nextPage, fetchMode.pageSize, forced)
        return@withContext when {
            responsePayload.isError -> {
                OnStatsFetched(responsePayload.error)
            }
            responsePayload.response != null -> {
                val replace = !fetchMode.loadMore
                sqlUtils.insert(siteModel, responsePayload.response, followerType, replaceExistingData = replace)
                val followerResponses = sqlUtils.selectAllFollowers(siteModel, followerType)
                val allFollowers = insightsMapper.mapAndMergeFollowersModels(
                        followerResponses,
                        followerType,
                        CacheMode.All
                )
                OnStatsFetched(allFollowers)
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getWpComFollowers(site: SiteModel, cacheMode: CacheMode): FollowersModel? {
        return getFollowers(site, WP_COM, cacheMode)
    }

    fun getEmailFollowers(site: SiteModel, cacheMode: CacheMode): FollowersModel? {
        return getFollowers(site, EMAIL, cacheMode)
    }

    private fun getFollowers(site: SiteModel, followerType: FollowerType, cacheMode: CacheMode): FollowersModel? {
        val followerResponses = sqlUtils.selectAllFollowers(site, followerType)
        return insightsMapper.mapAndMergeFollowersModels(followerResponses, followerType, cacheMode)
    }

    // Comments stats
    suspend fun fetchComments(siteModel: SiteModel, fetchMode: FetchMode, forced: Boolean = false) =
            withContext(coroutineContext) {
                val responsePayload = restClient.fetchTopComments(siteModel, forced = forced)
                return@withContext when {
                    responsePayload.isError -> {
                        OnStatsFetched(responsePayload.error)
                    }
                    responsePayload.response != null -> {
                        sqlUtils.insert(siteModel, responsePayload.response)
                        val cacheMode = if (fetchMode is FetchMode.Top)
                            CacheMode.Top(fetchMode.limit)
                        else
                            CacheMode.All
                        OnStatsFetched(insightsMapper.map(responsePayload.response, cacheMode))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getComments(site: SiteModel, cacheMode: CacheMode): CommentsModel? {
        return sqlUtils.selectCommentInsights(site)?.let { insightsMapper.map(it, cacheMode) }
    }

    // Tags
    suspend fun fetchTags(siteModel: SiteModel, fetchMode: FetchMode.Top, forced: Boolean = false) =
            withContext(coroutineContext) {
                val response = restClient.fetchTags(siteModel, max = fetchMode.limit + 1, forced = forced)
                return@withContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(
                                insightsMapper.map(response.response, CacheMode.Top(fetchMode.limit))
                        )
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getTags(site: SiteModel, cacheMode: CacheMode): TagsModel? {
        return sqlUtils.selectTags(site)?.let { insightsMapper.map(it, cacheMode) }
    }

    // Publicize stats
    suspend fun fetchPublicizeData(siteModel: SiteModel, pageSize: Int, forced: Boolean = false) =
            withContext(coroutineContext) {
                val response = restClient.fetchPublicizeData(siteModel, pageSize = pageSize + 1, forced = forced)
                return@withContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(insightsMapper.map(response.response, pageSize))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getPublicizeData(site: SiteModel, pageSize: Int): PublicizeModel? {
        return sqlUtils.selectPublicizeInsights(site)?.let { insightsMapper.map(it, pageSize) }
    }
}
