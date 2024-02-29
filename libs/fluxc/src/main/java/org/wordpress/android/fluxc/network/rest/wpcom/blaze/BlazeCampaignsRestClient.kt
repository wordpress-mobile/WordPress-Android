package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.AcceptHeaderStrategy
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.utils.extensions.putIfNotNull
import javax.inject.Inject
import javax.inject.Named

class BlazeCampaignsRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    acceptStrategy: AcceptHeaderStrategy.JsonAcceptHeader
) : BaseWPComRestClient(
    appContext,
    dispatcher,
    requestQueue,
    accessToken,
    userAgent,
    acceptStrategy
) {
    companion object {
        const val DEFAULT_ITEMS_LIMIT = 25 //Number of items to fetch in a single request
    }

    suspend fun fetchBlazeCampaigns(
        siteId: Long,
        skip: Int,
        limit: Int,
        locale: String,
        status: String? = null,
    ): BlazeCampaignsFetchedPayload<BlazeCampaignListResponse> {
        val url = "${WPCOMV2.sites.site(siteId).wordads.dsp.api.v1_1.campaigns}"
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mutableMapOf(
                "site_id" to siteId.toString(),
                "skip" to skip.toString(),
                "limit" to limit.toString(),
                "locale" to locale
            ).putIfNotNull("status" to status),
            BlazeCampaignListResponse::class.java
        )
        return when (response) {
            is Success -> BlazeCampaignsFetchedPayload(response.data)
            is Error -> BlazeCampaignsFetchedPayload(response.error.toBlazeCampaignsError())
        }
    }
}