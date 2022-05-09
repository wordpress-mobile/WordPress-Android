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
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SummaryRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchSummary(site: SiteModel, forced: Boolean): FetchStatsPayload<SummaryResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.summary.urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mapOf(),
            SummaryResponse::class.java,
            enableCaching = false,
            forced = forced
        )
        return when (response) {
            is Success -> FetchStatsPayload(response.data)
            is Error -> FetchStatsPayload(response.error.toStatsError())
        }
    }

    data class SummaryResponse(
        @SerializedName("likes") val likes: Int?,
        @SerializedName("comments") val comments: Int?,
        @SerializedName("followers") val followers: Int?
    )
}
