package org.wordpress.android.fluxc.network.rest.wpcom.stats.referrers

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
import org.wordpress.android.fluxc.network.utils.getInt
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.ReportReferrerAsSpamPayload
import org.wordpress.android.fluxc.store.toStatsError
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReferrersRestClient
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
    suspend fun fetchReferrers(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        pageSize: Int,
        forced: Boolean
    ): FetchStatsPayload<FetchReferrersResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.referrers.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "max" to pageSize.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                FetchReferrersResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                response.data.groups.values.forEach { it.groups.forEach { group -> group.build(gson) } }
                FetchStatsPayload(response.data)
            }
            is Error -> {
                FetchStatsPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun reportReferrerAsSpam(
        site: SiteModel,
        domain: String
    ): ReportReferrerAsSpamPayload<ReportReferrerAsSpamResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.referrers.spam.new_.urlV1_1
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                "$url?domain=$domain",
                HashMap(),
                ReportReferrerAsSpamResponse::class.java
        )
        return when (response) {
            is Success -> {
                ReportReferrerAsSpamPayload(response.data)
            }
            is Error -> {
                ReportReferrerAsSpamPayload(response.error.toStatsError())
            }
        }
    }

    suspend fun unreportReferrerAsSpam(
        site: SiteModel,
        domain: String
    ): ReportReferrerAsSpamPayload<ReportReferrerAsSpamResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.referrers.spam.delete.urlV1_1
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                "$url?domain=$domain",
                HashMap(),
                ReportReferrerAsSpamResponse::class.java
        )
        return when (response) {
            is Success -> {
                ReportReferrerAsSpamPayload(response.data)
            }
            is Error -> {
                ReportReferrerAsSpamPayload(response.error.toStatsError())
            }
        }
    }

    data class FetchReferrersResponse(
        @SerializedName("period") val statsGranularity: String?,
        @SerializedName("days") val groups: Map<String, Groups>
    ) {
        data class Groups(
            @SerializedName("other_views") val otherViews: Int?,
            @SerializedName("total_views") val totalViews: Int?,
            @SerializedName("groups") val groups: List<Group>
        )

        data class Group(
            @SerializedName("group") val groupId: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("total") val total: Int?,
            @SerializedName("results") val results: JsonElement?,
            @SerializedName("referrers") var referrers: List<Referrer>? = null,
            @SerializedName("views") var views: Int? = null,
            @SerializedName("spam") var spam: Boolean?
        ) {
            fun build(gson: Gson) {
                when (this.results) {
                    is JsonArray -> this.referrers = this.results.map {
                        gson.fromJson<Referrer>(
                                it,
                                Referrer::class.java
                        )
                    }
                    is JsonObject -> this.views = this.results.getInt("views")
                }
            }
        }

        data class Referrer(
            @SerializedName("group") val groupId: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("views") val views: Int?,
            @SerializedName("children") val children: List<Child>?,
            @SerializedName("spam") var spam: Boolean?
        )

        data class Child(
            @SerializedName("name") val name: String?,
            @SerializedName("views") val totals: Int?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("spam") var spam: Boolean?
        )
    }
}
