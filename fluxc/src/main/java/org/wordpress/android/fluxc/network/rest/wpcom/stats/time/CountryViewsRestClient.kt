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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CountryViewsRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchCountryViews(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        itemsToLoad: Int,
        forced: Boolean
    ): FetchStatsPayload<CountryViewsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.country_views.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to itemsToLoad.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                CountryViewsResponse::class.java,
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

    data class CountryViewsResponse(
        @SerializedName("country-info") val countryInfo: Map<String, CountryInfo>,
        @SerializedName("days") val days: Map<String, Day>
    ) {
        data class Day(
            @SerializedName("other_views") val otherViews: Int?,
            @SerializedName("total_views") val totalViews: Int?,
            @SerializedName("views") val views: List<CountryView>
        )

        data class CountryView(
            @SerializedName("country_code") val countryCode: String?,
            @SerializedName("views") val views: Int?
        )

        data class CountryInfo(
            @SerializedName("flag_icon") val flagIcon: String?,
            @SerializedName("flat_flag_icon") val flatFlagIcon: String?,
            @SerializedName("map_region") val mapRegion: String?,
            @SerializedName("country_full") val countryFull: String?
        )
    }
}
