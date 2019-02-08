package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CacheMode
import org.wordpress.android.fluxc.model.stats.CacheMode.All
import org.wordpress.android.fluxc.model.stats.FetchMode.Paged
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class FollowersStore @Inject constructor(
    private val restClient: FollowersRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
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
                        All
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
}
