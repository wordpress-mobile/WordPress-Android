package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class JetpackTunnelGsonRequestBuilder
@Inject constructor() {
    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
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
    suspend fun <T : Any> syncGetRequest(
        restClient: BaseWPComRestClient,
        site: SiteModel,
        url: String,
        params: Map<String, String>,
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false
    ) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
        val request = JetpackTunnelGsonRequest.buildGetRequest<T>(url, site.siteId, params, clazz, {
            cont.resume(JetpackSuccess(it))
        }, WPComErrorListener {
            cont.resume(JetpackError(it))
        }, {
            request: WPComGsonRequest<*> -> restClient.add(request)
        })
        cont.invokeOnCancellation {
            request?.cancel()
        }
        if (enableCaching) {
            request?.enableCaching(cacheTimeToLive)
        }
        if (forced) {
            request?.setShouldForceUpdate()
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
    fun <T : Any> buildPostRequest(
        site: SiteModel,
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>,
        listener: (T?) -> Unit,
        errorListener: WPComErrorListener
    ): WPComGsonRequest<JetpackTunnelResponse<T>>? {
        return JetpackTunnelGsonRequest.buildPostRequest(url, site.siteId, body, clazz, listener, errorListener)
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
        val request = JetpackTunnelGsonRequest.buildPostRequest<T>(url, site.siteId, body, clazz, {
            cont.resume(JetpackSuccess(it))
        }, WPComErrorListener {
            cont.resume(JetpackError(it))
        })
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
