package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import okhttp3.OkHttpClient
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.media.BaseWPV2MediaRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.tools.CoroutineEngine
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WPComV2MediaRestClient @Inject constructor(
    dispatcher: Dispatcher,
    coroutineEngine: CoroutineEngine,
    @Named("regular") okHttpClient: OkHttpClient,
    private val accessToken: AccessToken,
    private val wpComNetwork: WPComNetwork
) : BaseWPV2MediaRestClient(dispatcher, coroutineEngine, okHttpClient) {
    override fun WPAPIEndpoint.getFullUrl(site: SiteModel): String = getWPComUrl(site.siteId)

    override suspend fun getAuthorizationHeader(site: SiteModel): String = "Bearer ${accessToken.get()}"

    override suspend fun <T:Any> executeGetGsonRequest(
        site: SiteModel,
        endpoint: WPAPIEndpoint,
        params: Map<String, String>,
        clazz: Class<T>
    ): WPAPIResponse<T> {
        val url = endpoint.getFullUrl(site)

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            clazz = clazz,
            params = params,
        )

        return when(response) {
            is WPComGsonRequestBuilder.Response.Success -> WPAPIResponse.Success(response.data)
            is WPComGsonRequestBuilder.Response.Error -> WPAPIResponse.Error(
                WPAPINetworkError(response.error, response.error.apiError)
            )
        }
    }
}
