package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
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
class ClicksRestClient
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
    suspend fun fetchClicks(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<ClicksResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.clicks.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to pageSize.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                ClicksResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                response.data.groups.values.forEach {
                    it.clicks.forEach { group ->
                        group.build(gson)
                    }
                }
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    data class ClicksResponse(
        @SerializedName("period") val granularity: String?,
        @SerializedName("days") val groups: Map<String, Groups>
    ) {
        data class Groups(
            @SerializedName("other_clicks") val otherClicks: Int?,
            @SerializedName("total_clicks") val totalClicks: Int?,
            @SerializedName("clicks") val clicks: List<ClickGroup>
        )

        @Suppress("DataClassShouldBeImmutable")
        data class ClickGroup(
            @SerializedName("group") val groupId: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("views") val views: Int?,
            @SerializedName("children") val children: JsonElement?,
            @SerializedName("clicks") var clicks: List<Click>? = null
        ) {
            fun build(gson: Gson) {
                when (this.children) {
                    is JsonArray -> this.clicks = this.children.map {
                        gson.fromJson<Click>(
                                it,
                                Click::class.java
                        )
                    }
                }
            }
        }

        data class Click(
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("views") val views: Int?
        )
    }
}
