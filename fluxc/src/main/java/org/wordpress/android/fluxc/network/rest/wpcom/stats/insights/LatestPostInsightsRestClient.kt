package org.wordpress.android.fluxc.network.rest.wpcom.stats.insights

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
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LatestPostInsightsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchLatestPostForInsights(site: SiteModel, forced: Boolean): FetchStatsPayload<PostsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).posts.urlV1_1
        val params = mapOf(
                "order_by" to "date",
                "number" to "1",
                "type" to "post",
                "fields" to "ID,title,URL,discussion,like_count,date,featured_image"
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
            @SerializedName("discussion") val discussion: Discussion?,
            @SerializedName("featured_image") val featuredImage: String?
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
}
