package org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers

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
class EmailsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchEmailsSummary(
        site: SiteModel,
        quantity: Int,
        sortField: SortField,
        forced: Boolean
    ): FetchStatsPayload<EmailsSummaryResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.emails.summary.urlV1_1

        val params = mapOf(
            "quantity" to quantity.toString(),
            "sort_field" to sortField.toString(),
            "sort_order" to "desc"
        )

        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            params,
            EmailsSummaryResponse::class.java,
            enableCaching = false,
            forced = forced
        )
        return when (response) {
            is Success -> FetchStatsPayload(response.data)
            is Error -> FetchStatsPayload(response.error.toStatsError())
        }
    }

    enum class SortField(val sortField: String) { POST_ID("post_id"), OPENS("opens") }

    data class EmailsSummaryResponse(@SerializedName("posts") val posts: List<Post>) {
        data class Post(
            @SerializedName("id") val id: Long?,
            @SerializedName("href") val href: String?,
            @SerializedName("title") val title: String?,
            @SerializedName("opens") val opens: Int?,
            @SerializedName("clicks") val clicks: Int?
        )
    }
}
