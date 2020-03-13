package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import com.android.volley.Response.Listener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder.Response.Success
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
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME
    ) = suspendCancellableCoroutine<Response<T>> { cont ->
        val request = WPAPIGsonRequest(Method.GET, url, params, body, clazz, Listener {
            response -> cont.resume(Success(response))
        }, BaseErrorListener {
            error -> cont.resume(Response.Error(error))
        })
        cont.invokeOnCancellation {
            request.cancel()
        }
        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        restClient.add(request)
    }

    sealed class Response<T> {
        data class Success<T>(val data: T) : Response<T>()
        data class Error<T>(val error: BaseNetworkError) : Response<T>()
    }
}
