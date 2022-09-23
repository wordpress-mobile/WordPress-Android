package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.DefaultRetryPolicy
import com.android.volley.RetryPolicy
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class JetpackTunnelGsonRequestBuilder @Inject constructor() {
    companion object {
        const val DEFAULT_JETPACK_TUNNEL_TIMEOUT_MS = 15000
        const val DEFAULT_JETPACK_TUNNEL_MAX_RETRIES = 1
    }

    fun buildDefaultTimeoutRetryPolicy(
        timeout: Int = DEFAULT_JETPACK_TUNNEL_TIMEOUT_MS,
        retries: Int = DEFAULT_JETPACK_TUNNEL_MAX_RETRIES
    ): DefaultRetryPolicy =
        DefaultRetryPolicy(
            timeout,
            retries,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    @Suppress("LongParameterList")
    fun <T : Any> buildGetRequest(
        site: SiteModel,
        url: String,
        params: Map<String, String>,
        clazz: Class<T>,
        listener: (T?) -> Unit,
        errorListener: WPComErrorListener,
        jpTimeoutListener: ((WPComGsonRequest<*>) -> Unit)?
    ): WPComGsonRequest<JetpackTunnelResponse<T>>? {
        return JetpackTunnelGsonRequest.buildGetRequest(
            url,
            site.siteId,
            params,
            clazz,
            listener,
            errorListener,
            jpTimeoutListener
        )
    }

    /**
     * Creates a new GET request.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     */
    @Suppress("LongParameterList")
    suspend fun <T : Any> syncGetRequest(
        restClient: BaseWPComRestClient,
        site: SiteModel,
        url: String,
        params: Map<String, String>,
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false,
        retryPolicy: RetryPolicy? = null
    ) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
        val request = JetpackTunnelGsonRequest.buildGetRequest<T>(
            url,
            site.siteId,
            params,
            clazz,
            listener = { cont.resume(JetpackSuccess(it)) },
            errorListener = { cont.resume(JetpackError(it)) },
            jpTimeoutListener = { request: WPComGsonRequest<*> -> restClient.add(request) }
        )
        cont.invokeOnCancellation {
            request?.cancel()
        }
        if (enableCaching) {
            request?.enableCaching(cacheTimeToLive)
        }
        if (forced) {
            request?.setShouldForceUpdate()
        }
        retryPolicy?.let {
            AppLog.i(AppLog.T.API, "Timeout set to: ${it.currentTimeout}")
            request?.setRetryPolicy(it)
        }
        restClient.add(request)
    }

    /**
     * Creates a new JSON-formatted POST request.
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    @Suppress("LongParameterList")
    fun <T : Any> buildPostRequest(
        site: SiteModel,
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>,
        listener: (T?) -> Unit,
        errorListener: WPComErrorListener
    ): WPComGsonRequest<JetpackTunnelResponse<T>>? {
        return JetpackTunnelGsonRequest.buildPostRequest(
            url,
            site.siteId,
            body,
            clazz,
            listener,
            errorListener
        )
    }

    /**
     * Creates a new JSON-formatted POST request, triggers it and awaits results synchronously.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
     * @param clazz the class defining the expected response
     */
    suspend fun <T : Any> syncPostRequest(
        restClient: BaseWPComRestClient,
        site: SiteModel,
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>
    ) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
        val request = JetpackTunnelGsonRequest.buildPostRequest<T>(
            url,
            site.siteId, body,
            clazz,
            listener = { cont.resume(JetpackSuccess(it)) },
            errorListener = { cont.resume(JetpackError(it)) }
        )
        cont.invokeOnCancellation {
            request?.cancel()
        }
        restClient.add(request)
    }

    /**
     * Extends JetpackTunnelGsonRequestBuilder to make available a new JSON-formatted PUT requests,
     * triggers it and awaits results synchronously.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
     * @param clazz the class defining the expected response
     */

    suspend fun <T : Any> syncPutRequest(
        restClient: BaseWPComRestClient,
        site: SiteModel,
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>
    ) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
        val request = JetpackTunnelGsonRequest.buildPutRequest<T>(url, site.siteId, body, clazz,
            listener = { cont.resume(JetpackSuccess(it)) },
            errorListener = { cont.resume(JetpackError(it)) }
        )
        cont.invokeOnCancellation {
            request?.cancel()
        }
        restClient.add(request)
    }

    /**
     * Extends JetpackTunnelGsonRequestBuilder to make available a new JSON-formatted DELETE requests,
     * triggers it and awaits results synchronously.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param clazz the class defining the expected response
     */

    suspend fun <T : Any> syncDeleteRequest(
        restClient: BaseWPComRestClient,
        site: SiteModel,
        url: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
        val request = JetpackTunnelGsonRequest.buildDeleteRequest<T>(
            url,
            site.siteId,
            params,
            clazz,
            listener = { cont.resume(JetpackSuccess(it)) },
            errorListener = { cont.resume(JetpackError(it)) }
        )
        cont.invokeOnCancellation {
            request?.cancel()
        }
        restClient.add(request)
    }

    sealed class JetpackResponse<T> {
        data class JetpackSuccess<T>(val data: T?) : JetpackResponse<T>()
        data class JetpackError<T>(val error: WPComGsonNetworkError) : JetpackResponse<T>()
    }
}
