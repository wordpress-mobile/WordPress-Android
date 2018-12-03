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
import javax.inject.Singleton

@Singleton
class ReferrersRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchReferrers(
        site: SiteModel,
        period: StatsGranularity,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<ReferrersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.referrers.urlV1_1
        val params = mapOf(
                "period" to period.toString(),
                "max" to pageSize.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                ReferrersResponse::class.java,
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

    data class ReferrersResponse(
        @SerializedName("period") val period: String?,
        @SerializedName("days") val groups: Map<String, Groups>
    ) {
        data class Groups(
            @SerializedName("other_views") val otherViews: Int?,
            @SerializedName("total_views") val totalViews: Int?,
            @SerializedName("groups") val groups: List<Group>
        )

        data class Group(
            @SerializedName("group") val groupId: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("total") val total: Int?,
            @SerializedName("results") val results: List<Referrer>
        )

        data class Referrer(
            @SerializedName("group") val groupId: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("views") val views: Int?,
            @SerializedName("children") val children: List<Child>
        )

        data class Child(
            @SerializedName("name") val name: String?,
            @SerializedName("views") val totals: Int?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?
        )
    }
}
