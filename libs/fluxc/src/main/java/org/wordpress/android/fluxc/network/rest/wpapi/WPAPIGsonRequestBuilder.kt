package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.coroutines.resume

class WPAPIGsonRequestBuilder @Inject constructor() {
    suspend fun <T> syncGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        callMethod(Method.GET, url, params, body, clazz, cont, enableCaching, cacheTimeToLive, nonce, restClient)
    }
    suspend fun <T> syncGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        type: Type,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        callMethod(Method.GET, url, params, body, type, cont, enableCaching, cacheTimeToLive, nonce, restClient)
    }

    suspend fun <T> syncPostRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        callMethod(Method.POST, url, null, body, clazz, cont, false, 0, nonce, restClient)
    }

    suspend fun <T> syncPutRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        callMethod(Method.PUT, url, null, body, clazz, cont, false, 0, nonce, restClient)
    }

    suspend fun <T> syncDeleteRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        nonce: String? = null
    ) = suspendCancellableCoroutine<WPAPIResponse<T>> { cont ->
        callMethod(Method.DELETE, url, null, body, clazz, cont, false, 0, nonce, restClient)
    }

    @Suppress("LongParameterList")
    private fun <T> callMethod(
        method: Int,
        url: String,
        params: Map<String, String>? = null,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        cont: CancellableContinuation<WPAPIResponse<T>>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        nonce: String?,
        restClient: BaseWPAPIRestClient
    ) {
        val request = WPAPIGsonRequest(method, url, params, body, clazz, { response ->
            cont.resume(Success(response))
        }, { error ->
            cont.resume(Error(error))
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

    @Suppress("LongParameterList")
    private fun <T> callMethod(
        method: Int,
        url: String,
        params: Map<String, String>?,
        body: Map<String, Any>,
        type: Type,
        cont: CancellableContinuation<WPAPIResponse<T>>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        nonce: String?,
        restClient: BaseWPAPIRestClient
    ) {
        val request = WPAPIGsonRequest<T>(method, url, params, body, type, { response ->
            cont.resume(Success(response))
        }, { error ->
            cont.resume(Error(error))
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
