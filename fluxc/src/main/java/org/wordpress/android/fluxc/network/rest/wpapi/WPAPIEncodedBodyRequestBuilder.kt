package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import com.android.volley.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import javax.inject.Inject
import kotlin.coroutines.resume

class WPAPIEncodedBodyRequestBuilder @Inject constructor() {
    suspend fun syncPostRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME
    ) = suspendCancellableCoroutine<WPAPIResponse<String>> { cont ->
        val request = WPAPIEncodedBodyRequest(Method.POST, url, params, body, Response.Listener { response ->
            cont.resume(WPAPIResponse.Success(response))
        }, BaseRequest.BaseErrorListener { error ->
            cont.resume(WPAPIResponse.Error(error))
        })

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        restClient.add(request)
    }
}
