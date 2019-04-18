package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightTypeModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.persistence.InsightTypeSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType.AVERAGE_VIEWS_PER_DAY
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType.CLICKS_BY_WEEKS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType.MONTHS_AND_YEARS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType.POST_HEADER
import org.wordpress.android.fluxc.store.StatsStore.PostDetailType.POST_OVERVIEW
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.AUTHORS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.CLICKS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.COUNTRIES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.OVERVIEW
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.REFERRERS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.SEARCH_TERMS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.VIDEOS
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class StatsStore
@Inject constructor(
    private val coroutineContext: CoroutineContext,
    private val insightTypeSqlUtils: InsightTypeSqlUtils
) {
    private val defaultList = listOf(POSTING_ACTIVITY, TODAY_STATS, ALL_TIME_STATS, MOST_POPULAR_DAY_AND_HOUR, COMMENTS)

    suspend fun getInsights(site: SiteModel): List<InsightType> = withContext(coroutineContext) {
        val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
        val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)

        return@withContext if (addedInsights.isEmpty() && removedInsights.isEmpty()) {
            defaultList
        } else {
            addedInsights
        }
    }

    suspend fun getInsightsManagementModel(site: SiteModel) = withContext(coroutineContext) {
        val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
        val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)

        return@withContext if (addedInsights.isEmpty() && removedInsights.isEmpty()) {
            InsightTypeModel(defaultList, InsightType.values().filter { !defaultList.contains(it) })
        } else {
            InsightTypeModel(addedInsights, removedInsights)
        }
    }

    suspend fun updateTypes(site: SiteModel, model: InsightTypeModel) = withContext(coroutineContext) {
        insertOrReplaceItems(site, model.addedTypes, model.removedTypes)
    }

    suspend fun moveTypeUp(site: SiteModel, type: InsightType) {
        val insightTypes = getInsights(site)
        val indexOfMovedItem = insightTypes.indexOf(type)
        if (indexOfMovedItem > 0 && indexOfMovedItem < insightTypes.size) {
            val updatedInsights = mutableListOf<InsightType>()
            val switchedItemIndex = indexOfMovedItem - 1
            if (indexOfMovedItem > 1) {
                updatedInsights.addAll(insightTypes.subList(0, switchedItemIndex))
            }
            updatedInsights.add(type)
            updatedInsights.add(insightTypes[switchedItemIndex])
            if (indexOfMovedItem + 1 < insightTypes.size) {
                updatedInsights.addAll(insightTypes.subList(indexOfMovedItem + 1, insightTypes.size))
            }
            insightTypeSqlUtils.insertOrReplaceAddedItems(site, updatedInsights)
        }
    }

    suspend fun moveTypeDown(site: SiteModel, type: InsightType) {
        val insightTypes = getInsights(site)
        val indexOfMovedItem = insightTypes.indexOf(type)
        if (indexOfMovedItem >= 0 && indexOfMovedItem < insightTypes.size - 1) {
            val updatedInsights = mutableListOf<InsightType>()
            val switchedItemIndex = indexOfMovedItem + 1
            if (indexOfMovedItem > 0) {
                updatedInsights.addAll(insightTypes.subList(0, indexOfMovedItem))
            }
            updatedInsights.add(insightTypes[switchedItemIndex])
            updatedInsights.add(type)
            if (switchedItemIndex + 1 < insightTypes.size) {
                updatedInsights.addAll(insightTypes.subList(switchedItemIndex + 1, insightTypes.size))
            }
            insightTypeSqlUtils.insertOrReplaceAddedItems(site, updatedInsights)
        }
    }

    suspend fun removeType(site: SiteModel, type: InsightType) = withContext(coroutineContext) {
        val insightsModel = getInsightsManagementModel(site)
        val addedItems = insightsModel.addedTypes.filter { it != type }
        val removedItems = insightsModel.removedTypes + listOf(type)
        insertOrReplaceItems(site, addedItems, removedItems)
    }

    suspend fun addType(site: SiteModel, type: InsightType) = withContext(coroutineContext) {
        val insightsModel = getInsightsManagementModel(site)
        val addedItems = insightsModel.addedTypes + listOf(type)
        val removedItems = insightsModel.removedTypes.filter { it != type }
        insertOrReplaceItems(site, addedItems, removedItems)
    }

    private fun insertOrReplaceItems(
        site: SiteModel,
        addedItems: List<InsightType>,
        removedItems: List<InsightType>
    ) {
        insightTypeSqlUtils.insertOrReplaceAddedItems(site, addedItems)
        insightTypeSqlUtils.insertOrReplaceRemovedItems(site, removedItems)
    }

    suspend fun getTimeStatsTypes(): List<TimeStatsType> = withContext(coroutineContext) {
        return@withContext listOf(
                OVERVIEW,
                POSTS_AND_PAGES,
                REFERRERS,
                CLICKS,
                AUTHORS,
                COUNTRIES,
                SEARCH_TERMS,
                VIDEOS
        )
    }

    suspend fun getPostDetailTypes(): List<PostDetailType> = withContext(coroutineContext) {
        return@withContext listOf(
                POST_HEADER,
                POST_OVERVIEW,
                MONTHS_AND_YEARS,
                AVERAGE_VIEWS_PER_DAY,
                CLICKS_BY_WEEKS
        )
    }

    interface StatsType

    enum class InsightType : StatsType {
        LATEST_POST_SUMMARY,
        MOST_POPULAR_DAY_AND_HOUR,
        ALL_TIME_STATS,
        FOLLOWER_TOTALS,
        TAGS_AND_CATEGORIES,
        ANNUAL_SITE_STATS,
        COMMENTS,
        FOLLOWERS,
        TODAY_STATS,
        POSTING_ACTIVITY,
        PUBLICIZE
    }

    enum class TimeStatsType : StatsType {
        OVERVIEW,
        POSTS_AND_PAGES,
        REFERRERS,
        CLICKS,
        AUTHORS,
        COUNTRIES,
        SEARCH_TERMS,
        PUBLISHED,
        VIDEOS
    }

    enum class PostDetailType : StatsType {
        POST_HEADER,
        POST_OVERVIEW,
        MONTHS_AND_YEARS,
        AVERAGE_VIEWS_PER_DAY,
        CLICKS_BY_WEEKS
    }

    data class OnStatsFetched<T>(val model: T? = null, val cached: Boolean = false) : Store.OnChanged<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class FetchStatsPayload<T>(
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

    class StatsError(var type: StatsErrorType, var message: String? = null) : OnChangedError
}

fun WPComGsonNetworkError.toStatsError(): StatsError {
    val type = when (type) {
        TIMEOUT -> StatsErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> StatsErrorType.API_ERROR
        PARSE_ERROR,
        NOT_FOUND,
        CENSORED,
        INVALID_RESPONSE -> StatsErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> StatsErrorType.AUTHORIZATION_REQUIRED
        UNKNOWN,
        null -> GENERIC_ERROR
    }
    return StatsError(type, message)
}
