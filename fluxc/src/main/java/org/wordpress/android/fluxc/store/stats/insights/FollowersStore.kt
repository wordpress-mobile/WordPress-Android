package org.wordpress.android.fluxc.store.stats.insights

import android.util.Log
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.All
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.persistence.toDbKey
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
        fetchMode: PagedMode,
        forced: Boolean = false
    ): OnStatsFetched<FollowersModel> {
        return fetchFollowers(siteModel, forced, WP_COM, fetchMode)
    }

    suspend fun fetchEmailFollowers(
        siteModel: SiteModel,
        fetchMode: PagedMode,
        forced: Boolean = false
    ): OnStatsFetched<FollowersModel> {
        return fetchFollowers(siteModel, forced, EMAIL, fetchMode)
    }

    private suspend fun fetchFollowers(
        siteModel: SiteModel,
        forced: Boolean = false,
        followerType: FollowerType,
        fetchMode: PagedMode
    ) = withContext(coroutineContext) {
        Log.d(
                "followers_log",
                "Fetching followers: forced - $forced, type - $followerType, fetchedItems: ${fetchMode.pageSize}, loadMore: ${fetchMode.loadMore}"
        )
        if (!forced && !fetchMode.loadMore && sqlUtils.hasFreshRequest(
                        siteModel,
                        followerType.toDbKey(),
                        fetchMode.pageSize
                )) {
            Log.d("followers_log", "Returns fresh data from DB instead")
            return@withContext OnStatsFetched(
                    getFollowers(
                            siteModel,
                            followerType,
                            cacheMode = LimitMode.Top(fetchMode.pageSize)
                    ),
                    cached = true
            )
        }
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
                Log.d("followers_log", "Successfully fetched data")
                sqlUtils.insert(
                        siteModel,
                        responsePayload.response,
                        followerType,
                        replaceExistingData = replace,
                        requestedItems = fetchMode.pageSize
                )
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

    fun getWpComFollowers(site: SiteModel, cacheMode: LimitMode): FollowersModel? {
        return getFollowers(site, WP_COM, cacheMode)
    }

    fun getEmailFollowers(site: SiteModel, cacheMode: LimitMode): FollowersModel? {
        return getFollowers(site, EMAIL, cacheMode)
    }

    private fun getFollowers(site: SiteModel, followerType: FollowerType, cacheMode: LimitMode): FollowersModel? {
        val followerResponses = sqlUtils.selectAllFollowers(site, followerType)
        Log.d("followers_log",
                "Loading data from the db: followerType - $followerType, limit: ${(cacheMode as? LimitMode.Top)?.limit
                        ?: -1}"
        )
        return insightsMapper.mapAndMergeFollowersModels(followerResponses, followerType, cacheMode)
    }
}
