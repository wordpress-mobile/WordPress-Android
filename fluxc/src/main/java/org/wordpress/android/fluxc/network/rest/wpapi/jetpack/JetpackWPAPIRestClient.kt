package org.wordpress.android.fluxc.network.rest.wpapi.jetpack

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIEncodedBodyRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JetpackWPAPIRestClient @Inject constructor(
    private val wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
    dispatcher: Dispatcher,
    @Named("custom-ssl") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchJetpackConnectionUrl(
        site: SiteModel,
        nonce: Nonce?
    ): JetpackConnectionUrlPayload {
        val baseUrl = site.wpApiRestUrl ?: "${site.url}/wp-json"
        val url = "${baseUrl.trimEnd('/')}/${JPAPI.connection.url.pathV4.trimStart('/')}"

        val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            nonce = nonce?.value
        )

        return when (response) {
            is Success<String> -> JetpackConnectionUrlPayload(response.data)
            is Error -> JetpackConnectionUrlPayload(response.error)
        }
    }

    data class JetpackConnectionUrlPayload(
        val response: String?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError): this(null) {
            this.error = error
        }
    }
}
