package org.wordpress.android.fluxc.network.rest.wpcom.stats

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
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
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAllTimeInsights(site: SiteModel, forced: Boolean): FetchStatsPayload<AllTimeResponse> {
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
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean): FetchStatsPayload<MostPopularResponse> {
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
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchLatestPostForInsights(site: SiteModel, forced: Boolean): FetchStatsPayload<PostsResponse> {
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
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchPostStats(
        site: SiteModel,
        postId: Long,
        forced: Boolean
    ): FetchStatsPayload<PostStatsResponse> {
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
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchTimePeriodStats(
        site: SiteModel,
        period: StatsGranularity,
        date: Date,
        forced: Boolean
    ): FetchStatsPayload<VisitResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1

        val params = mapOf(
                "unit" to period.toString(),
                "quantity" to "1",
                "date" to statsUtils.getFormattedDate(date)
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
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchFollowers(
        site: SiteModel,
        type: FollowerType,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<FollowersResponse> {
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
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchTopComments(
        site: SiteModel,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<CommentsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.comments.urlV1_1

        val params = mapOf(
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CommentsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchTags(
        site: SiteModel,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<TagsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.tags.urlV1_1

        val params = mapOf(
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                TagsResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun fetchPublicizeData(
        site: SiteModel,
        pageSize: Int = 6,
        forced: Boolean
    ): FetchStatsPayload<PublicizeResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.publicize.urlV1_1

        val params = mapOf("max" to pageSize.toString())
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PublicizeResponse::class.java,
                enableCaching = true,
                forced = forced
        )
        return when (response) {
            is Success -> {
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    data class AllTimeResponse(
        @SerializedName("date") var date: Date? = null,
        @SerializedName("stats") val stats: StatsResponse?
    ) {
        data class StatsResponse(
            @SerializedName("visitors") val visitors: Int?,
            @SerializedName("views") val views: Int?,
            @SerializedName("posts") val posts: Int?,
            @SerializedName("views_best_day") val viewsBestDay: String?,
            @SerializedName("views_best_day_total") val viewsBestDayTotal: Int?
        )
    }

    data class MostPopularResponse(
        @SerializedName("highest_day_of_week") val highestDayOfWeek: Int?,
        @SerializedName("highest_hour") val highestHour: Int?,
        @SerializedName("highest_day_percent") val highestDayPercent: Double?,
        @SerializedName("highest_hour_percent") val highestHourPercent: Double?
    )

    data class PostsResponse(
        @SerializedName("found") val postsFound: Int? = 0,
        @SerializedName("posts") val posts: List<PostResponse> = listOf()
    ) {
        data class PostResponse(
            @SerializedName("ID") val id: Long,
            @SerializedName("title") val title: String?,
            @SerializedName("date") val date: Date?,
            @SerializedName("URL") val url: String?,
            @SerializedName("like_count") val likeCount: Int?,
            @SerializedName("discussion") val discussion: Discussion?
        ) {
            data class Discussion(
                @SerializedName("comment_count") val commentCount: Int?
            )
        }
    }

    data class PostStatsResponse(
        @SerializedName("highest_month") val highestMonth: Int = 0,
        @SerializedName("highest_day_average") val highestDayAverage: Int = 0,
        @SerializedName("highest_week_average") val highestWeekAverage: Int = 0,
        @SerializedName("views") val views: Int?,
        @SerializedName("date") val date: String? = null,
        @SerializedName("data") val data: List<List<String>>?,
        @SerializedName("fields") val fields: List<String>?,
        @SerializedName("weeks") val weeks: List<Week>,
        @SerializedName("years") val years: Map<Int, Year>,
        @SerializedName("averages") val averages: Map<Int, Average>

    ) {
        data class Year(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("total") val total: Int?
        )

        data class Week(
            @SerializedName("average") val average: Int?,
            @SerializedName("total") val total: Int?,
            @SerializedName("days") val days: List<Day>
        )

        data class Day(
            @SerializedName("day") val day: String,
            @SerializedName("count") val count: Int?
        )

        data class Average(
            @SerializedName("months") val months: Map<Int, Int>,
            @SerializedName("overall") val overall: Int?
        )
    }

    data class VisitResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("unit") val unit: String?,
        @SerializedName("fields") val fields: List<String>,
        @SerializedName("data") val data: List<List<String>>
    )

    data class CommentsResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("monthly_comments") val monthlyComments: Int?,
        @SerializedName("total_comments") val totalComments: Int?,
        @SerializedName("most_active_day") val mostActiveDay: String?,
        @SerializedName("authors") val authors: List<Author>?,
        @SerializedName("posts") val posts: List<Post>?
    ) {
        data class Author(
            @SerializedName("name") val name: String?,
            @SerializedName("link") val link: String?,
            @SerializedName("gravatar") val gravatar: String?,
            @SerializedName("comments") val comments: Int?
        )

        data class Post(
            @SerializedName("name") val name: String?,
            @SerializedName("link") val link: String?,
            @SerializedName("id") val id: Long?,
            @SerializedName("comments") val comments: Int?
        )
    }

    enum class FollowerType(val path: String) {
        EMAIL("email"), WP_COM("wpcom")
    }

    data class FollowersResponse(
        @SerializedName("page") val page: Int?,
        @SerializedName("pages") val pages: Int?,
        @SerializedName("total") val total: Int?,
        @SerializedName("total_email") val totalEmail: Int?,
        @SerializedName("total_wpcom") val totalWpCom: Int?,
        @SerializedName("subscribers") val subscribers: List<FollowerResponse>

    ) {
        data class FollowerResponse(
            @SerializedName("label") val label: String?,
            @SerializedName("avatar") val avatar: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("date_subscribed") val dateSubscribed: Date?,
            @SerializedName("follow_data") val followData: FollowData?
        )
    }

    data class FollowData(
        @SerializedName("type") val type: String?,
        @SerializedName("params") val params: FollowParams?
    ) {
        data class FollowParams(
            @SerializedName("follow-text") val followText: String?,
            @SerializedName("following-text") val followingText: String?,
            @SerializedName("following-hover-text") val followingHoverText: String?,
            @SerializedName("is_following") val isFollowing: Boolean?,
            @SerializedName("blog_id") val blogId: String?,
            @SerializedName("site_id") val siteId: String?,
            @SerializedName("stats-source") val statsSource: String?,
            @SerializedName("blog_domain") val blogDomain: String?
        )
    }

    data class TagsResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("tags") val tags: List<TagsGroup>
    ) {
        data class TagsGroup(
            @SerializedName("views") val views: Long?,
            @SerializedName("tags") val tags: List<TagResponse>
        ) {
            data class TagResponse(
                @SerializedName("name") val name: String?,
                @SerializedName("type") val type: String?,
                @SerializedName("link") val link: String?
            )
        }
    }

    data class PublicizeResponse(
        @SerializedName("services") val services: List<Service>
    ) {
        data class Service(
            @SerializedName("service") val service: String,
            @SerializedName("followers") val followers: Int
        )
    }
}
