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
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import org.wordpress.android.fluxc.utils.SiteUtils
import javax.inject.Singleton

@Singleton
class TodayInsightsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchTimePeriodStats(
        site: SiteModel,
        period: StatsGranularity,
        forced: Boolean
    ): FetchStatsPayload<VisitResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1

        val params = mapOf(
                "unit" to period.toString(),
                "quantity" to "1",
                "date" to statsUtils.getFormattedDate(timeZone = SiteUtils.getNormalizedTimezone(site.timezone))
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

    data class VisitResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("unit") val unit: String?,
        @SerializedName("fields") val fields: List<String>,
        @SerializedName("data") val data: List<List<String>>
    )
}
