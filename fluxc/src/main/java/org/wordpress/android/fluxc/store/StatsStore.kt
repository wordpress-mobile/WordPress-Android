package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
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
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.ALL_TIME_STATS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.COMMENTS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.FOLLOWERS
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.LATEST_POST_SUMMARY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.MOST_POPULAR_DAY_AND_HOUR
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.POSTING_ACTIVITY
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.PUBLICIZE
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TAGS_AND_CATEGORIES
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes.TODAY_STATS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes.AVERAGE_VIEWS_PER_DAY
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes.CLICKS_BY_WEEKS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes.MONTHS_AND_YEARS
import org.wordpress.android.fluxc.store.StatsStore.PostDetailTypes.POST_OVERVIEW
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.AUTHORS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.CLICKS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.COUNTRIES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.OVERVIEW
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.POSTS_AND_PAGES
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.REFERRERS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.SEARCH_TERMS
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes.VIDEOS
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class StatsStore
@Inject constructor(private val coroutineContext: CoroutineContext) {
    suspend fun getInsights(): List<InsightsTypes> = withContext(coroutineContext) {
        return@withContext listOf(
                LATEST_POST_SUMMARY,
                TODAY_STATS,
                ALL_TIME_STATS,
                MOST_POPULAR_DAY_AND_HOUR,
                COMMENTS,
                TAGS_AND_CATEGORIES,
                FOLLOWERS,
                POSTING_ACTIVITY,
                PUBLICIZE
        )
    }

    suspend fun getTimeStatsTypes(): List<TimeStatsTypes> = withContext(coroutineContext) {
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

    suspend fun getPostDetailTypes(): List<PostDetailTypes> = withContext(coroutineContext) {
        return@withContext listOf(
                POST_OVERVIEW,
                MONTHS_AND_YEARS,
                AVERAGE_VIEWS_PER_DAY,
                CLICKS_BY_WEEKS
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
        POSTS_AND_PAGES,
        REFERRERS,
        CLICKS,
        AUTHORS,
        COUNTRIES,
        SEARCH_TERMS,
        PUBLISHED,
        VIDEOS
    }

    enum class PostDetailTypes : StatsTypes {
        POST_OVERVIEW,
        MONTHS_AND_YEARS,
        AVERAGE_VIEWS_PER_DAY,
        CLICKS_BY_WEEKS
    }

    data class OnStatsFetched<T>(val model: T? = null) : Store.OnChanged<StatsError>() {
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

    class StatsError(var type: StatsErrorType, var message: String? = null) : Store.OnChangedError
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
