package org.wordpress.android.fluxc.network.rest.wpcom.stats.time

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
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.Referrer
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse.ReferrerGroup
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
    ): FetchStatsPayload<ReferrersResponse> {
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
                UnparsedReferrersResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                val firstGroup = response.data.dataForPeriod.values.firstOrNull()
                val parsedGroups = firstGroup?.unparsedReferrerGroups?.map {
                    it.parse(gson)
                } ?: listOf()
                FetchStatsPayload(
                        ReferrersResponse(
                                response.data.statsGranularity,
                                firstGroup?.otherViews,
                                firstGroup?.totalViews,
                                parsedGroups
                        )
                )
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
        val params = mapOf(
                "domain" to domain
        )
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                null,
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
        val params = mapOf(
                "domain" to domain
        )
        val response = wpComGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                null,
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

    data class UnparsedReferrersResponse(
        @SerializedName("period") val statsGranularity: String?,
        @SerializedName("days") val dataForPeriod: Map<String, PeriodData>
    ) {
        data class PeriodData(
            @SerializedName("other_views") val otherViews: Int?,
            @SerializedName("total_views") val totalViews: Int?,
            @SerializedName("groups") val unparsedReferrerGroups: List<UnparsedReferrerGroup>
        )

        data class UnparsedReferrerGroup(
            @SerializedName("group") val group: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("total") val total: Int?,
            @SerializedName("results") val results: JsonElement?
        ) {
            fun parse(gson: Gson): ReferrerGroup {
                var referrers: List<Referrer>? = null
                var views: Int? = null
                when (this.results) {
                    is JsonArray -> referrers = this.results.map {
                        gson.fromJson(
                                it,
                                Referrer::class.java
                        )
                    }
                    is JsonObject -> views = this.results.getInt("views")
                }
                return ReferrerGroup(group, name, icon, url, total, referrers, views, false)
            }
        }
    }

    data class ReferrersResponse(
        val statsGranularity: String?,
        val otherViews: Int?,
        val totalViews: Int?,
        val referrerGroups: List<ReferrerGroup>
    ) {
        data class ReferrerGroup(
            val group: String?,
            val name: String?,
            val icon: String?,
            val url: String?,
            val total: Int?,
            val referrers: List<Referrer>? = null,
            val views: Int? = null,
            val markedAsSpam: Boolean
        )

        data class Referrer(
            @SerializedName("group") val group: String?,
            @SerializedName("name") val name: String?,
            @SerializedName("icon") val icon: String?,
            @SerializedName("url") val url: String?,
            @SerializedName("views") val views: Int?,
            @SerializedName("child") val children: List<Child>?,
            @SerializedName("markedAsSpam") val markedAsSpam: Boolean
        )

        data class Child(
            @SerializedName("url") val url: String?
        )
    }

    class ReportReferrerAsSpamResponse(val success: Boolean) : Response
}
