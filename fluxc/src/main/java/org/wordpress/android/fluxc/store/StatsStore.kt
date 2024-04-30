package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.fluxc.persistence.StatsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_GROW
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_REMINDER
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ACTION_SCHEDULE
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TOTAL_LIKES
import org.wordpress.android.fluxc.store.StatsStore.InsightType.VIEWS_AND_VISITORS
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.FILE_DOWNLOADS
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import org.wordpress.android.util.AppLog
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

val DEFAULT_INSIGHTS = listOf(
        MOST_POPULAR_DAY_AND_HOUR,
        ALL_TIME_STATS,
        TODAY_STATS,
        FOLLOWERS
)

val JETPACK_DEFAULT_INSIGHTS = listOf(
        VIEWS_AND_VISITORS,
        TOTAL_LIKES,
        TOTAL_COMMENTS,
        TOTAL_FOLLOWERS,
        MOST_POPULAR_DAY_AND_HOUR,
        LATEST_POST_SUMMARY
)

val STATS_UNAVAILABLE_WITH_JETPACK = listOf(FILE_DOWNLOADS)
const val INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN = "INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN"

@Singleton
class StatsStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val insightTypeSqlUtils: InsightTypeSqlUtils,
    private val preferenceUtils: PreferenceUtilsWrapper,
    private val statsSqlUtils: StatsSqlUtils
) {
    fun deleteAllData() {
        statsSqlUtils.deleteAllStats()
    }

    fun deleteSiteData(site: SiteModel) {
        statsSqlUtils.deleteSiteStats(site)
    }

    suspend fun getInsightTypes(site: SiteModel): List<StatsType> =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "getInsightTypes") {
                val types = mutableListOf<StatsType>()
/**
 * Customize Insights Management card is being hidden for now.
 * It will be updated to new design in the next iteration.
 * Also, make sure to remove @Ignore annotation on tests in StatsStoreTest when this is undone.
 **/
//                if (!preferenceUtils.getFluxCPreferences().getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, false)) {
//                    types.add(ManagementType.NEWS_CARD)
//                }
                types.addAll(getAddedInsights(site))
                types.add(ManagementType.CONTROL)
                return@withDefaultContext types
            }

    fun hideInsightsManagementNewsCard() = coroutineEngine.run(AppLog.T.STATS, this, "hideInsightsManagementNewsCard") {
        preferenceUtils.getFluxCPreferences().edit().putBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true).apply()
    }

    fun isInsightsManagementNewsCardShowing() =
            coroutineEngine.run(AppLog.T.STATS, this, "isInsightsManagementNewsCardShowing") {
                preferenceUtils.getFluxCPreferences().getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true)
            }

    suspend fun getAddedInsights(site: SiteModel) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "getAddedInsights") {
                val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
                val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)

                return@withDefaultContext if (addedInsights.isEmpty() && removedInsights.isEmpty()) {
                    DEFAULT_INSIGHTS
                } else {
                    addedInsights
                }
            }

    fun getRemovedInsights(addedInsights: List<InsightType>) =
            coroutineEngine.run(AppLog.T.STATS, this, "getRemovedInsights") {
                InsightType.values().asList() - addedInsights
            }

    suspend fun updateTypes(site: SiteModel, addedInsights: List<InsightType>) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "updateTypes") {
                insertOrReplaceItems(site, addedInsights, getRemovedInsights(addedInsights))
            }

    suspend fun moveTypeUp(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "moveTypeUp") {
                val insights = getAddedInsights(site)
                val index = insights.indexOf(type)

                if (index > 0) {
                    Collections.swap(insights, index, index - 1)
                    insightTypeSqlUtils.insertOrReplaceAddedItems(site, insights)
                }
            }

    suspend fun moveTypeDown(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "moveTypeDown") {
                val insights = getAddedInsights(site)
                val index = insights.indexOf(type)

                if (index < insights.size - 1) {
                    Collections.swap(insights, index, index + 1)
                    insightTypeSqlUtils.insertOrReplaceAddedItems(site, insights)
                }
            }

    suspend fun removeType(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "removeType") {
                val addedItems = getAddedInsights(site) - type
                updateTypes(site, addedItems)
            }

    suspend fun addType(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "addType") {
                val addedItems = getAddedInsights(site) + type
                updateTypes(site, addedItems)
            }

    suspend fun addActionType(site: SiteModel, type: InsightType) =
        coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "addActionType($type)") {
            val addedInsights = getAddedInsights(site).toMutableList()

            when (type) {
                ACTION_REMINDER -> {
                    if (!addedInsights.contains(ACTION_REMINDER)) {
                        addedInsights.add(addedInsights.indexOf(MOST_POPULAR_DAY_AND_HOUR) + 1, ACTION_REMINDER)
                    }
                }
                ACTION_GROW -> {
                    if (!addedInsights.contains(ACTION_GROW)) {
                        addedInsights.add(addedInsights.indexOf(TOTAL_FOLLOWERS) + 1, ACTION_GROW)
                    }
                }
                ACTION_SCHEDULE -> {
                    if (!addedInsights.contains(ACTION_SCHEDULE) && !addedInsights.contains(ACTION_REMINDER)) {
                        addedInsights.add(addedInsights.indexOf(MOST_POPULAR_DAY_AND_HOUR) + 1, ACTION_SCHEDULE)
                    }
                }
                else -> {
                    // just to make when exhaustive
                }
            }

            insightTypeSqlUtils.insertOrReplaceAddedItems(site, addedInsights)
        }

    suspend fun removeActionType(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "removeActionType($type)") {
                val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
                val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)
                insertOrReplaceItems(site, addedInsights - type, removedInsights + type)
            }

    suspend fun isActionTypeShown(site: SiteModel, type: InsightType) =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "isActionTypeShown(${site.id} $type") {
                val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
                val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)

                return@withDefaultContext (addedInsights.contains(type) || removedInsights.contains(type))
            }

    private fun insertOrReplaceItems(
        site: SiteModel,
        addedItems: List<InsightType>,
        removedItems: List<InsightType>
    ) {
        insightTypeSqlUtils.insertOrReplaceAddedItems(site, addedItems)
        insightTypeSqlUtils.insertOrReplaceRemovedItems(site, removedItems)
    }

    suspend fun getTimeStatsTypes(site: SiteModel): List<TimeStatsType> =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "getTimeStatsTypes") {
                return@withDefaultContext if (site.isJetpackConnected) {
                    TimeStatsType.values().toList().filter { !STATS_UNAVAILABLE_WITH_JETPACK.contains(it) }
                } else {
                    TimeStatsType.values().toList()
                }
            }

    suspend fun getSubscriberTypes() = coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "getSubscriberTypes") {
        return@withDefaultContext SubscriberType.values().toList()
    }

    suspend fun getPostDetailTypes(): List<PostDetailType> =
            coroutineEngine.withDefaultContext(AppLog.T.STATS, this, "getPostDetailTypes") {
                return@withDefaultContext PostDetailType.values().toList()
            }

    interface StatsType

    enum class InsightType : StatsType {
        VIEWS_AND_VISITORS,
        TOTAL_LIKES,
        TOTAL_COMMENTS,
        TOTAL_FOLLOWERS,
        LATEST_POST_SUMMARY,
        MOST_POPULAR_DAY_AND_HOUR,
        ALL_TIME_STATS,
        FOLLOWER_TYPES,
        FOLLOWER_TOTALS,
        TAGS_AND_CATEGORIES,
        ANNUAL_SITE_STATS,
        COMMENTS,
        AUTHORS_COMMENTS,
        POSTS_COMMENTS,
        FOLLOWERS,
        TODAY_STATS,
        POSTING_ACTIVITY,
        PUBLICIZE,
        ACTION_GROW,
        ACTION_REMINDER,
        ACTION_SCHEDULE
    }

    enum class ManagementType : StatsType {
        NEWS_CARD,
        CONTROL
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
        VIDEOS,
        FILE_DOWNLOADS
    }


    enum class SubscriberType : StatsType { TOTAL_SUBSCRIBERS, SUBSCRIBERS_CHART, SUBSCRIBERS, EMAILS }

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

    data class OnReportReferrerAsSpam<T>(val model: T? = null) : Store.OnChanged<StatsError>() {
        constructor(error: StatsError) : this() {
            this.error = error
        }
    }

    data class ReportReferrerAsSpamPayload<T>(
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
        INVALID_RESPONSE,
        ALREADY_SPAMMED
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
        UNKNOWN -> if (message == "Already spammed.") StatsErrorType.ALREADY_SPAMMED else GENERIC_ERROR
        null -> GENERIC_ERROR
    }
    return StatsError(type, message)
}
