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
class FollowersRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchFollowers(
        site: SiteModel,
        type: FollowerType,
        page: Int,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<FollowersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.followers.urlV1_1

        val params = mutableMapOf(
                "type" to type.path,
                "max" to pageSize.toString()
        )

        if (page > 1) {
            params["page"] = page.toString()
        }

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

    enum class FollowerType(val path: String) {
        ALL("all"), EMAIL("email"), WP_COM("wpcom")
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
}
