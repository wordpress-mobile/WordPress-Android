package org.wordpress.android.fluxc.network.rest.wpcom.stats

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsAllTimeModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.InsightsStore.FetchAllTimeInsightsPayload
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
        val response = wpComGsonRequestBuilder.syncGetRequest(this, url, params, InsightsAllTimeResponse::class.java)
        return when (response) {
            is Success -> {
                val data = response.data
                val stats = data.stats
                FetchAllTimeInsightsPayload(
                        InsightsAllTimeModel(
                                data.date,
                                stats.visitors,
                                stats.views,
                                stats.posts,
                                stats.viewsBestDay,
                                stats.viewsBestDayTotal
                        )
                )
            }
            is Error -> {
                FetchAllTimeInsightsPayload(StatsError(GENERIC_ERROR, response.error.message))
            }
        }
    }

    data class InsightsAllTimeResponse(
        @SerializedName("date")
        var date: String? = null,
        @SerializedName("stats")
        val stats: StatsResponse
    )

    data class StatsResponse(
        @SerializedName("visitors")
        val visitors: Int,
        @SerializedName("views")
        val views: Int,
        @SerializedName("posts")
        val posts: Int,
        @SerializedName("views_best_day")
        val viewsBestDay: String,
        @SerializedName("views_best_day_total")
        val viewsBestDayTotal: Int
    )
}
