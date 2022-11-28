package org.wordpress.android.fluxc.network.rest.wpapi.site

import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject
import javax.inject.Named

class SiteWPAPIRestClient @Inject constructor(
    private val wpapiAuthenticator: WPAPIAuthenticator,
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {

    suspend fun fetchWPAPISite(
        payload: FetchWPAPISitePayload
    ): SiteModel {
        return wpapiAuthenticator.makeAuthenticatedWPAPIRequest(
            siteUrl = payload.url,
            username = payload.username,
            password = payload.password
        ) { wpApiUrl, nonce ->
            val url = wpApiUrl.slashJoin(WPAPI.settings.urlV2)
            val result = wpapiGsonRequestBuilder.syncGetRequest(
                restClient = this,
                url = url,
                clazz = WPSiteSettingsResponse::class.java,
                nonce = nonce?.value
            )

            return@makeAuthenticatedWPAPIRequest when (result) {
                is Success -> {
                    SiteModel().apply {
                        name = result.data?.title
                        description = result.data?.description
                        timezone = result.data?.timezone
                        email = result.data?.email
                        showOnFront = result.data?.showOnFront
                        pageOnFront = result.data?.pageOnFront ?: 0
                        origin = SiteModel.ORIGIN_WPAPI
                        this.url = result.data?.url ?: payload.url
                        this.username = payload.username
                        this.password = payload.password
                    }
                }
                is Error -> {
                    SiteModel().apply {
                        error = result.error
                    }
                }
            }
        }
    }

    suspend fun fetchWPAPISite(
        site: SiteModel
    ): SiteModel {
        return fetchWPAPISite(
            payload = FetchWPAPISitePayload(
                username = site.username,
                password = site.password,
                url = site.url
            )
        )
    }

    private data class WPSiteSettingsResponse(
        @SerializedName("title") val title: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("timezone") val timezone: String? = null,
        @SerializedName("email") val email: String? = null,
        @SerializedName("url") val url: String? = null,
        @SerializedName("show_on_front") val showOnFront: String? = null,
        @SerializedName("page_on_front") val pageOnFront: Long? = null
    )
}

