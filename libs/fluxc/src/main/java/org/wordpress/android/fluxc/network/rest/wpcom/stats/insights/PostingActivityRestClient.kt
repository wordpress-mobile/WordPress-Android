package org.wordpress.android.fluxc.network.rest.wpcom.stats.insights

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
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

private const val MAX_ITEMS = 3000

@Singleton
class PostingActivityRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchPostingActivity(
        site: SiteModel,
        startDay: Day,
        endDay: Day,
        forced: Boolean
    ): FetchStatsPayload<PostingActivityResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.streak.urlV1_1
        val params = mapOf(
                "startDate" to statsUtils.getFormattedDate(startDay),
                "endDate" to statsUtils.getFormattedDate(endDay),
                "gmtOffset" to 0.toString(),
                "max" to MAX_ITEMS.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                PostingActivityResponse::class.java,
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

    data class PostingActivityResponse(
        @SerializedName("streak") val streak: Streaks?,
        @SerializedName("data") val data: Map<Long, Int>?
    ) {
        data class Streaks(
            @SerializedName("long") val longStreak: Streak?,
            @SerializedName("current") val currentStreak: Streak?
        )

        data class Streak(
            @SerializedName("start") val start: String?,
            @SerializedName("end") val end: String?,
            @SerializedName("length") val length: Int?
        )
    }
}
