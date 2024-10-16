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
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.toStatsError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TagsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    val statsUtils: StatsUtils
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchTags(
        site: SiteModel,
        max: Int,
        forced: Boolean
    ): FetchStatsPayload<TagsResponse> {
        val url = WPCOMREST.sites.site(site.siteId).stats.tags.urlV1_1

        val params = mapOf(
                "max" to max.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                TagsResponse::class.java,
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

    data class TagsResponse(
        @SerializedName("date") val date: String?,
        @SerializedName("tags") val tags: List<TagsGroup>
    ) {
        data class TagsGroup(
            @SerializedName("views") val views: Long?,
            @SerializedName("tags") val tags: List<TagResponse>
        ) {
            data class TagResponse(
                @SerializedName("name") val name: String?,
                @SerializedName("type") val type: String?,
                @SerializedName("link") val link: String?
            )
        }
    }
}
