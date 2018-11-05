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
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.network.utils.getFormattedDate
import org.wordpress.android.fluxc.store.InsightsStore.FetchInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import java.util.Date
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

    suspend fun fetchPostStats(
        site: SiteModel,
        postId: Long,
        forced: Boolean
    ): FetchInsightsPayload<PostStatsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.post.item(postId).urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
                PostStatsResponse::class.java,
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

    suspend fun fetchTimePeriodStats(
        site: SiteModel,
        period: StatsGranularity,
        date: Date,
        forced: Boolean
    ): FetchInsightsPayload<VisitResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1

        val params = mapOf(
                "unit" to period.toPath(),
                "quantity" to "1",
                "date" to getFormattedDate(site, date, period)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                VisitResponse::class.java,
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

    suspend fun fetchFollowers(
        site: SiteModel,
        type: FollowerType,
        pageSize: Int = 6,
        forced: Boolean
    ): FetchInsightsPayload<FollowersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.followers.urlV1_1

        val params = mapOf(
                "type" to type.path,
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                FollowersResponse::class.java,
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
        @SerializedName("date") var date: Date? = null,
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
            @SerializedName("date") val date: Date,
            @SerializedName("URL") val url: String,
            @SerializedName("like_count") val likeCount: Int,
            @SerializedName("discussion") val discussion: Discussion?
        ) {
            data class Discussion(
                @SerializedName("comment_count") val commentCount: Int
            )
        }
    }

    data class PostStatsResponse(
        @SerializedName("highest_month") val highestMonth: Int = 0,
        @SerializedName("highest_day_average") val highestDayAverage: Int = 0,
        @SerializedName("highest_week_average") val highestWeekAverage: Int = 0,
        @SerializedName("views") val views: Int,
        @SerializedName("date") val date: String? = null,
        @SerializedName("data") val data: List<List<String>>?,
        @SerializedName("fields") val fields: List<String>?,
        @SerializedName("weeks") val weeks: List<Week>,
        @SerializedName("years") val years: Map<Int, Year>,
        @SerializedName("averages") val averages: Map<Int, Average>

    ) {
        data class Year(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("total") val total: Int
        )

        data class Week(
            @SerializedName("average") val average: Int,
            @SerializedName("total") val total: Int,
            @SerializedName("days") val days: List<Day>
        )

        data class Day(
            @SerializedName("day") val day: String,
            @SerializedName("count") val count: Int
        )

        data class Average(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("overall") val overall: Int
        )
    }

    data class VisitResponse(
        @SerializedName("date") val date: String,
        @SerializedName("unit") val unit: String,
        @SerializedName("fields") val fields: List<String>,
        @SerializedName("data") val data: List<List<String>>
    )

    private fun StatsGranularity.toPath(): String {
        return when (this) {
            DAYS -> "day"
            WEEKS -> "week"
            MONTHS -> "month"
            YEARS -> "year"
        }
    }

    enum class FollowerType(val path: String) {
        EMAIL("email"), WP_COM("wpcom")
    }

    data class FollowersResponse(
        @SerializedName("page") val page: Int,
        @SerializedName("pages") val pages: Int,
        @SerializedName("total") val total: Int,
        @SerializedName("total_email") val totalEmail: Int,
        @SerializedName("total_wpcom") val totalWpCom: Int,
        @SerializedName("subscribers") val subscribers: List<FollowerResponse>

    ) {
        data class FollowerResponse(
            @SerializedName("label") val label: String,
            @SerializedName("avatar") val avatar: String,
            @SerializedName("url") val url: String,
            @SerializedName("date_subscribed") val dateSubscribed: Date,
            @SerializedName("follow_data") val followData: FollowData
        )

        data class FollowData(
            @SerializedName("type") val type: String,
            @SerializedName("params") val params: FollowParams
        ) {
            data class FollowParams(
                @SerializedName("follow-text") val followText: String,
                @SerializedName("following-text") val followingText: String,
                @SerializedName("following-hover-text") val followingHoverText: String,
                @SerializedName("is_following") val isFollowing: Boolean,
                @SerializedName("blog_id") val blogId: String,
                @SerializedName("site_id") val siteId: String,
                @SerializedName("stats-source") val statsSource: String,
                @SerializedName("blog_domain") val blogDomain: String
            )
        }
    }
}
