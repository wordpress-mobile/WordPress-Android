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
class FileDownloadsRestClient
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
    suspend fun fetchFileDownloads(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        itemsToLoad: Int,
        forced: Boolean
    ): FetchStatsPayload<FileDownloadsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.file_downloads.urlV1_1
        val params = mapOf(
                "period" to granularity.toString(),
                "num" to itemsToLoad.toString(),
                "date" to statsUtils.getFormattedDate(date)
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                FileDownloadsResponse::class.java,
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

    data class FileDownloadsResponse(
        @SerializedName("period") val statsGranularity: String?,
        @SerializedName("date") val date: String?,
        @SerializedName("days") val groups: Map<String, Group>
    ) {
        data class Group(
            @SerializedName("other_downloads") val otherDownloads: Int?,
            @SerializedName("total_downloads") val totalDownloads: Int?,
            @SerializedName("files") val files: List<File>
        )

        data class File(
            @SerializedName("filename") val filename: String?,
            @SerializedName("downloads") var downloads: Int?
        )
    }
}
