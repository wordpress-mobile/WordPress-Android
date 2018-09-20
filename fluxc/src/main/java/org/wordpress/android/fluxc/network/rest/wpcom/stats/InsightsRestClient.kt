package org.wordpress.android.fluxc.network.rest.wpcom.stats

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
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
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.InsightsStore.FetchInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import javax.inject.Singleton

@Singleton
class InsightsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<AllTimeResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.urlV1_1

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                AllTimeResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<MostPopularResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.insights.urlV1_1

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                MostPopularResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchLatestPostForInsights(site: SiteModel, forced: Boolean): FetchInsightsPayload<PostsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).posts.urlV1_1
        val params = mapOf(
                "order_by" to "date",
                "number" to "1",
                "type" to "post",
                "fields" to "ID,title,URL,discussion,like_count,date"
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PostsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    suspend fun fetchPostViewsForInsights(site: SiteModel, postId: Long): FetchInsightsPayload<PostViewsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.post.item(postId).urlV1_1

        val params = mapOf("fields" to "views")
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, PostViewsResponse::class.java)
        return when (response) {
            is Success -> {
                FetchInsightsPayload(response.data)
            }
            is Error -> {
                FetchInsightsPayload(buildStatsError(response.error))
            }
        }
    }

    private fun buildStatsError(error: WPComGsonNetworkError): StatsError {
        val type = when (error.type) {
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
        return StatsError(type, error.message)
    }

    data class AllTimeResponse(
        @SerializedName("date") var date: String? = null,
        @SerializedName("stats") val stats: StatsResponse
    ) {
        data class StatsResponse(
            @SerializedName("visitors") val visitors: Int,
            @SerializedName("views") val views: Int,
            @SerializedName("posts") val posts: Int,
            @SerializedName("views_best_day") val viewsBestDay: String,
            @SerializedName("views_best_day_total") val viewsBestDayTotal: Int
        )
    }

    data class MostPopularResponse(
        @SerializedName("highest_day_of_week") val highestDayOfWeek: Int,
        @SerializedName("highest_hour") val highestHour: Int,
        @SerializedName("highest_day_percent") val highestDayPercent: Double,
        @SerializedName("highest_hour_percent") val highestHourPercent: Double
    )

    data class PostsResponse(
        @SerializedName("found") val postsFound: Int = 0,
        @SerializedName("posts") val posts: List<PostResponse> = listOf()
    ) {
        data class PostResponse(
            @SerializedName("ID") val id: Long,
            @SerializedName("title") val title: String,
            @SerializedName("date") val date: String,
            @SerializedName("URL") val url: String,
            @SerializedName("like_count") val likeCount: Int,
            @SerializedName("discussion") val discussion: Discussion?
        ) {
            data class Discussion(
                @SerializedName("comment_count") val commentCount: Int
            )
        }
    }

    data class PostViewsResponse(@SerializedName("views") val views: Int)
}
