package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.utils.extensions.putIfNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlazeCampaignsRestClient @Inject constructor(
    private val wpComNetwork: WPComNetwork
) {
    companion object {
        const val DEFAULT_PER_PAGE = 25 // Number of items to fetch in a single request
    }

    suspend fun fetchBlazeCampaigns(
        siteId: Long,
        offset: Int,
        perPage: Int,
        locale: String,
        status: String? = null,
    ): BlazeCampaignsFetchedPayload<BlazeCampaignListResponse> {
        val url = WPCOMV2.sites.site(siteId).wordads.dsp.api.v1_1.campaigns.url
        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = mutableMapOf(
                "site_id" to siteId.toString(),
                "skip" to offset.toString(),
                "limit" to perPage.toString(),
                "locale" to locale
            ).putIfNotNull("status" to status),
            clazz = BlazeCampaignListResponse::class.java
        )
        return when (response) {
            is Success -> BlazeCampaignsFetchedPayload(response.data)
            is Error -> BlazeCampaignsFetchedPayload(response.error.toBlazeCampaignsError())
        }
    }
}
