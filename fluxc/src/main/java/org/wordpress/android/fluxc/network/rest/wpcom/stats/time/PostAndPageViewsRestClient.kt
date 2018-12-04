package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

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
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import java.util.Date
import javax.inject.Singleton

@Singleton
class PostAndPageViewsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchPostAndPageViews(
        site: SiteModel,
        period: StatsGranularity,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<PostAndPageViewsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.top_posts.urlV1_1
        val params = mapOf(
                "period" to period.toString(),
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PostAndPageViewsResponse::class.java,
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

    data class PostAndPageViewsResponse(
        @SerializedName("date") var date: Date? = null,
        @SerializedName("days") val days: Map<String, ViewsResponse>,
        @SerializedName("period") val period: String
    ) {
        data class ViewsResponse(
            @SerializedName("postviews") val postViews: List<PostViewsResponse>,
            @SerializedName("total_views") val totalViews: Int
        ) {
            data class PostViewsResponse(
                @SerializedName("id") val id: Long,
                @SerializedName("title") val title: String,
                @SerializedName("type") val type: String,
                @SerializedName("href") val href: String,
                @SerializedName("views") val views: Int
            )
        }
    }
}
