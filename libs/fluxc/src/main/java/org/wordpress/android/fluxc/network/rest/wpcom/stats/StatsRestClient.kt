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
import org.wordpress.android.fluxc.store.InsightsStore.FetchAllTimeInsightsPayload
import org.wordpress.android.fluxc.store.InsightsStore.FetchLatestPostPayload
import org.wordpress.android.fluxc.store.InsightsStore.FetchLatestPostViewsPayload
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Singleton

@Singleton
class StatsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchAllTimeInsights(site: SiteModel): FetchAllTimeInsightsPayload {
        val url = WPCOMREST.sites.site(site.siteId).stats.urlV1_1

        val params = mapOf<String, String>()
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, AllTimeResponse::class.java)
        return when (response) {
            is Success -> {
                FetchAllTimeInsightsPayload(response.data)
            }
            is Error -> {
                FetchAllTimeInsightsPayload(StatsError(GENERIC_ERROR, response.error.message))
            }
        }
    }

    suspend fun fetchLatestPostForInsights(site: SiteModel): FetchLatestPostPayload {
        val url = WPCOMREST.sites.site(site.siteId).posts.urlV1_1
        val params = mapOf(
                "order_by" to "date",
                "number" to "1",
                "type" to "post",
                "fields" to "ID,title,URL,discussion,like_count,date"
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, PostsResponse::class.java)
        return when (response) {
            is Success -> {
                FetchLatestPostPayload(response.data)
            }
            is Error -> {
                FetchLatestPostPayload(StatsError(GENERIC_ERROR, response.error.message))
            }
        }
    }

    suspend fun fetchPostViewsForInsights(site: SiteModel, postId: Long): FetchLatestPostViewsPayload {
        val url = WPCOMREST.sites.site(site.siteId).stats.post.item(postId).urlV1_1

        val params = mapOf("fields" to "views")
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, PostViewsResponse::class.java)
        return when (response) {
            is Success -> {
                FetchLatestPostViewsPayload(response.data)
            }
            is Error -> {
                FetchLatestPostViewsPayload(StatsError(GENERIC_ERROR, response.error.message))
            }
        }
    }

    data class AllTimeResponse(
        @SerializedName("date") var date: String? = null,
        @SerializedName("stats") val stats: StatsResponse
    )

    data class StatsResponse(
        @SerializedName("visitors") val visitors: Int,
        @SerializedName("views") val views: Int,
        @SerializedName("posts") val posts: Int,
        @SerializedName("views_best_day") val viewsBestDay: String,
        @SerializedName("views_best_day_total") val viewsBestDayTotal: Int
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

    private fun periodDateMaxParams(
        period: StatsTimeframe,
        date: String,
        maxResultsRequested: Int
    ): Map<String, String> {
        return mapOf("period" to period.value, "date" to date, "max" to maxResultsRequested.toString())
    }

    fun load(
        blogId: Long,
        timeframe: StatsTimeframe,
        date: String,
        sectionToUpdate: StatsEndpointsEnum,
        maxResultsRequested: Int,
        pageRequested: Int
    ) {
        val period = timeframe.value
        val periodDateMaxPlaceholder = "?period=%s&date=%s&max=%s"
        var path = String.format(Locale.US, "/sites/%s/stats/" + sectionToUpdate.restEndpointPath, blogId)
        when (sectionToUpdate) {
            StatsEndpointsEnum.VISITS -> path = String.format(
                    Locale.US,
                    "$path?unit=%s&quantity=15&date=%s", period, date
            )
            StatsEndpointsEnum.TOP_POSTS, StatsEndpointsEnum.REFERRERS, StatsEndpointsEnum.CLICKS, StatsEndpointsEnum.GEO_VIEWS, StatsEndpointsEnum.AUTHORS, StatsEndpointsEnum.VIDEO_PLAYS, StatsEndpointsEnum.SEARCH_TERMS -> path = String.format(
                    Locale.US, path + periodDateMaxPlaceholder, period, date, maxResultsRequested
            )
            StatsEndpointsEnum.TAGS_AND_CATEGORIES, StatsEndpointsEnum.PUBLICIZE -> path = String.format(
                    Locale.US, "$path?max=%s", maxResultsRequested
            )
            StatsEndpointsEnum.COMMENTS -> {
            }
            StatsEndpointsEnum.FOLLOWERS_WPCOM -> if (pageRequested < 1) {
                path = String.format(Locale.US, "$path&max=%s", maxResultsRequested)
            } else {
                path = String.format(
                        Locale.US, "$path&period=%s&date=%s&max=%s&page=%s",
                        period, date, maxResultsRequested, pageRequested
                )
            }
            StatsEndpointsEnum.FOLLOWERS_EMAIL -> if (pageRequested < 1) {
                path = String.format(Locale.US, "$path&max=%s", maxResultsRequested)
            } else {
                path = String.format(
                        Locale.US, "$path&period=%s&date=%s&max=%s&page=%s",
                        period, date, maxResultsRequested, pageRequested
                )
            }
            StatsEndpointsEnum.COMMENT_FOLLOWERS -> if (pageRequested < 1) {
                path = String.format(Locale.US, "$path?max=%s", maxResultsRequested)
            } else {
                path = String.format(
                        Locale.US, "$path?period=%s&date=%s&max=%s&page=%s", period,
                        date, maxResultsRequested, pageRequested
                )
            }
            StatsEndpointsEnum.INSIGHTS_ALL_TIME, StatsEndpointsEnum.INSIGHTS_POPULAR -> {
            }
            StatsEndpointsEnum.INSIGHTS_TODAY -> path = String.format(
                    Locale.US,
                    "$path?period=day&date=%s",
                    date
            )
            StatsEndpointsEnum.INSIGHTS_LATEST_POST_SUMMARY ->
                // This is an edge cases since we're not loading stats but posts
                path = String.format(
                        Locale.US, "/sites/%s/%s", blogId,
                        sectionToUpdate.restEndpointPath + "?order_by=date&number=1&type=post&fields=ID,title,URL,discussion,like_count,date"
                )
            StatsEndpointsEnum.INSIGHTS_LATEST_POST_VIEWS ->
                // This is a kind of edge case, since we used the pageRequested parameter to request a single postID
                path = String.format(Locale.US, "$path/%s?fields=views", pageRequested)
            else -> {
                AppLog.i(T.STATS, "Called an update of Stats of unknown section!?? " + sectionToUpdate.name)
                return
            }
        }
    }

    enum class StatsTimeframe(val value: String) {
        INSIGHTS("day"), DAY("day"), WEEK("week"), MONTH("month"), YEAR("year");
    }
}
