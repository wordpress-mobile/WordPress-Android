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
class SearchTermsRestClient
@Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchSearchTerms(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        itemsToLoad: Int,
        forced: Boolean
    ): FetchStatsPayload<SearchTermsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.search_terms.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to itemsToLoad.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                SearchTermsResponse::class.java,
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

    data class SearchTermsResponse(
        @SerializedName("period") val granularity: String?,
        @SerializedName("days") val days: Map<String, Day>
    ) {
        data class Day(
            @SerializedName("encrypted_search_terms") val encryptedSearchTerms: Int?,
            @SerializedName("other_search_terms") val otherSearchTerms: Int?,
            @SerializedName("total_search_terms") val totalSearchTimes: Int?,
            @SerializedName("search_terms") val searchTerms: List<SearchTerm>
        )

        data class SearchTerm(
            @SerializedName("term") val term: String?,
            @SerializedName("views") val views: Int?
        )
    }
}
