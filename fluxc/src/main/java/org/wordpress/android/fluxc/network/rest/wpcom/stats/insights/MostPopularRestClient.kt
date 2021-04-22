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
class MostPopularRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchMostPopularInsights(site: SiteModel, forced: Boolean): FetchStatsPayload<MostPopularResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.insights.urlV1_1

        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                mapOf(),
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

    data class MostPopularResponse(
        @SerializedName("highest_day_of_week") val highestDayOfWeek: Int?,
        @SerializedName("highest_hour") val highestHour: Int?,
        @SerializedName("highest_day_percent") val highestDayPercent: Double?,
        @SerializedName("highest_hour_percent") val highestHourPercent: Double?,
        @SerializedName("years") val yearInsightResponses: List<YearInsightsResponse>?
    ) {
        data class YearInsightsResponse(
            @SerializedName("avg_comments") val avgComments: Double?,
            @SerializedName("avg_images") val avgImages: Double?,
            @SerializedName("avg_likes") val avgLikes: Double?,
            @SerializedName("avg_words") val avgWords: Double?,
            @SerializedName("total_comments") val totalComments: Int,
            @SerializedName("total_images") val totalImages: Int,
            @SerializedName("total_likes") val totalLikes: Int,
            @SerializedName("total_posts") val totalPosts: Int,
            @SerializedName("total_words") val totalWords: Int,
            @SerializedName("year") val year: String
        )
    }
}
