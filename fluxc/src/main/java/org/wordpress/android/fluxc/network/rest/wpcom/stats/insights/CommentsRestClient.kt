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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommentsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchTopComments(
        site: SiteModel,
        forced: Boolean
    ): FetchStatsPayload<CommentsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.comments.urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                emptyMap(),
                CommentsResponse::class.java,
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
}
