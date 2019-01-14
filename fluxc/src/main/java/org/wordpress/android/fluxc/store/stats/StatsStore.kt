package org.wordpress.android.fluxc.store.stats

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightTypeDataModel.Status.REMOVED
import org.wordpress.android.fluxc.model.stats.InsightTypesModel
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
import org.wordpress.android.fluxc.persistence.stats.InsightTypesSqlUtils
import org.wordpress.android.fluxc.store.Store.OnChanged
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsError
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.stats.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.AUTHORS
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.CLICKS
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.COUNTRIES
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.DATE
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.SEARCH_TERMS
import org.wordpress.android.fluxc.store.stats.StatsStore.TimeStatsTypes.VIDEOS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class StatsStore
@Inject constructor(
    private val coroutineContext: CoroutineContext,
    private val insightTypesSqlUtils: InsightTypesSqlUtils
) {
    private val defaultList = listOf(LATEST_POST_SUMMARY, TODAY_STATS, ALL_TIME_STATS, POSTING_ACTIVITY)

    suspend fun getInsights(site: SiteModel): List<InsightsTypes> = withContext(coroutineContext) {
        val cachedData = insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)

        return@withContext if (cachedData.isNotEmpty()) {
            cachedData
        } else {
            defaultList
        }
    }

    suspend fun getInsightsManagementModel(site: SiteModel) = withContext(coroutineContext) {
        val cachedData = insightTypesSqlUtils.selectAddedItemsOrderedByStatus(site)
        return@withContext if (cachedData.isNotEmpty()) {
            InsightTypesModel(cachedData, insightTypesSqlUtils.selectRemovedItemsOrderedByStatus(site))
        } else {
            InsightTypesModel(defaultList, InsightsTypes.values().filter { !defaultList.contains(it) })
        }
    }

    suspend fun updateTypes(site: SiteModel, model: InsightTypesModel) = withContext(coroutineContext) {
        insightTypesSqlUtils.insertOrReplaceAddedItems(site, model.addedTypes)
        insightTypesSqlUtils.insertOrReplaceRemovedItems(site, model.removedTypes)
    }

    suspend fun moveTypeUp(site: SiteModel, type: InsightsTypes) {
        val insightTypes = getInsights(site)
        val indexOfMovedItem = insightTypes.indexOf(type)
        if (indexOfMovedItem > 0 && indexOfMovedItem < insightTypes.size) {
            val updatedInsights = mutableListOf<InsightsTypes>()
            val switchedItemIndex = indexOfMovedItem - 1
            if (indexOfMovedItem > 1) {
                updatedInsights.addAll(insightTypes.subList(0, switchedItemIndex))
            }
            updatedInsights.add(type)
            updatedInsights.add(insightTypes[switchedItemIndex])
            if (indexOfMovedItem + 1 < insightTypes.size) {
                updatedInsights.addAll(insightTypes.subList(indexOfMovedItem + 1, insightTypes.size))
            }
            insightTypesSqlUtils.updatePositions(site, updatedInsights)
        }
    }

    suspend fun moveTypeDown(site: SiteModel, type: InsightsTypes) {
        val insightTypes = getInsights(site)
        val indexOfMovedItem = insightTypes.indexOf(type)
        if (indexOfMovedItem >= 0 && indexOfMovedItem < insightTypes.size - 1) {
            val updatedInsights = mutableListOf<InsightsTypes>()
            val switchedItemIndex = indexOfMovedItem + 1
            if (indexOfMovedItem > 0) {
                updatedInsights.addAll(insightTypes.subList(0, indexOfMovedItem))
            }
            updatedInsights.add(insightTypes[switchedItemIndex])
            updatedInsights.add(type)
            if (switchedItemIndex + 1 < insightTypes.size) {
                updatedInsights.addAll(insightTypes.subList(switchedItemIndex + 1, insightTypes.size))
            }
            insightTypesSqlUtils.updatePositions(site, updatedInsights)
        }
    }

    suspend fun removeType(site: SiteModel, type: InsightsTypes) = withContext(coroutineContext) {
        insightTypesSqlUtils.updateStatus(site, type, REMOVED)
    }

    suspend fun getTimeStatsTypes(): List<TimeStatsTypes> = withContext(coroutineContext) {
        return@withContext listOf(
                OVERVIEW,
                DATE,
                POSTS_AND_PAGES,
                REFERRERS,
                CLICKS,
                AUTHORS,
                COUNTRIES,
                SEARCH_TERMS,
                VIDEOS
        )
    }

    interface StatsTypes

    enum class InsightsTypes : StatsTypes {
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

    enum class TimeStatsTypes : StatsTypes {
        OVERVIEW,
        DATE,
        POSTS_AND_PAGES,
        REFERRERS,
        CLICKS,
        AUTHORS,
        COUNTRIES,
        SEARCH_TERMS,
        PUBLISHED,
        VIDEOS
    }

    data class OnStatsFetched<T>(val model: T? = null) : OnChanged<StatsError>() {
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
