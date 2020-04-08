package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import com.android.volley.Response.Listener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import javax.inject.Inject
import kotlin.coroutines.resume

class WPAPIGsonRequestBuilder @Inject constructor() {
    suspend fun <T> syncGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String>,
        body: Map<String, String>,
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        val request = WPAPIGsonRequest(Method.GET, url, params, body, clazz, Listener {
            response -> cont.resume(Success(response))
        }, BaseErrorListener {
            error -> cont.resume(WPAPIResponse.Error(error))
        })

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        if (nonce != null) {
            request.addHeader("x-wp-nonce", nonce)
        }

        restClient.add(request)
    }
}
