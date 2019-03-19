package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class VideoPlaysRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val gson: Gson,
    private val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchVideoPlays(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        itemsToLoad: Int,
        forced: Boolean
    ): FetchStatsPayload<VideoPlaysResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.video_plays.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to itemsToLoad.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                VideoPlaysResponse::class.java,
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

    data class VideoPlaysResponse(
        @SerializedName("period") val granularity: String?,
        @SerializedName("days") val days: Map<String, Days>
    ) {
        data class Days(
            @SerializedName("other_plays") val otherPlays: Int?,
            @SerializedName("total_plays") val totalPlays: Int?,
            @SerializedName("plays") val plays: List<Play>
        )

        data class Play(
            @SerializedName("post_id") val postId: String?,
            @SerializedName("title") val title: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("plays") val plays: Int?
        )
    }
}
