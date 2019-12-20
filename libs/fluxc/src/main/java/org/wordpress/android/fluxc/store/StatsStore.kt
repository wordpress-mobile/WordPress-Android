package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
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
import org.wordpress.android.fluxc.store.StatsStore.InsightType.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.FOLLOWER_TOTALS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightType.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightType.values
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsType.FILE_DOWNLOADS
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

val DEFAULT_INSIGHTS = listOf(LATEST_POST_SUMMARY, TODAY_STATS, ALL_TIME_STATS, FOLLOWER_TOTALS)
val STATS_UNAVAILABLE_WITH_JETPACK = listOf(FILE_DOWNLOADS)
const val INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN = "INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN"

@Singleton
class StatsStore
@Inject constructor(
    private val coroutineContext: CoroutineContext,
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

    suspend fun getInsightTypes(site: SiteModel): List<StatsType> = withContext(coroutineContext) {
        val types = mutableListOf<StatsType>()
        if (!preferenceUtils.getFluxCPreferences().getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, false)) {
            types.add(ManagementType.NEWS_CARD)
        }
        types.addAll(getAddedInsights(site))
        types.add(ManagementType.CONTROL)
        return@withContext types
    }

    fun hideInsightsManagementNewsCard() {
        preferenceUtils.getFluxCPreferences().edit().putBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true).apply()
    }

    fun isInsightsManagementNewsCardShowing(): Boolean {
        return preferenceUtils.getFluxCPreferences().getBoolean(INSIGHTS_MANAGEMENT_NEWS_CARD_SHOWN, true)
    }

    suspend fun getAddedInsights(site: SiteModel) = withContext(coroutineContext) {
        val addedInsights = insightTypeSqlUtils.selectAddedItemsOrderedByStatus(site)
        val removedInsights = insightTypeSqlUtils.selectRemovedItemsOrderedByStatus(site)

        return@withContext if (addedInsights.isEmpty() && removedInsights.isEmpty()) {
            DEFAULT_INSIGHTS
        } else {
            addedInsights
        }
    }

    fun getRemovedInsights(addedInsights: List<InsightType>): List<InsightType> {
        return values().asList() - addedInsights
    }

    suspend fun updateTypes(site: SiteModel, addedInsights: List<InsightType>) = withContext(coroutineContext) {
        insertOrReplaceItems(site, addedInsights, getRemovedInsights(addedInsights))
    }

    suspend fun moveTypeUp(site: SiteModel, type: InsightType) {
        val insights = getAddedInsights(site)
        val index = insights.indexOf(type)

        if (index > 0) {
            Collections.swap(insights, index, index - 1)
            insightTypeSqlUtils.insertOrReplaceAddedItems(site, insights)
        }
    }

    suspend fun moveTypeDown(site: SiteModel, type: InsightType) {
        val insights = getAddedInsights(site)
        val index = insights.indexOf(type)

        if (index < insights.size - 1) {
            Collections.swap(insights, index, index + 1)
            insightTypeSqlUtils.insertOrReplaceAddedItems(site, insights)
        }
    }

    suspend fun removeType(site: SiteModel, type: InsightType) = withContext(coroutineContext) {
        val addedItems = getAddedInsights(site) - type
        updateTypes(site, addedItems)
    }

    suspend fun addType(site: SiteModel, type: InsightType) = withContext(coroutineContext) {
        val addedItems = getAddedInsights(site) + type
        updateTypes(site, addedItems)
    }

    private fun insertOrReplaceItems(
        site: SiteModel,
        addedItems: List<InsightType>,
        removedItems: List<InsightType>
    ) {
        insightTypeSqlUtils.insertOrReplaceAddedItems(site, addedItems)
        insightTypeSqlUtils.insertOrReplaceRemovedItems(site, removedItems)
    }

    suspend fun getTimeStatsTypes(site: SiteModel): List<TimeStatsType> = withContext(coroutineContext) {
        return@withContext if (site.isJetpackConnected) {
            TimeStatsType.values().toList().filter { !STATS_UNAVAILABLE_WITH_JETPACK.contains(it) }
        } else {
            TimeStatsType.values().toList()
        }
    }

    suspend fun getPostDetailTypes(): List<PostDetailType> = withContext(coroutineContext) {
        return@withContext PostDetailType.values().toList()
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
