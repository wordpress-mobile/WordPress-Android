package org.wordpress.android.fluxc.network.xmlrpc

import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Success
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class XMLRPCRequestBuilder
@Inject constructor() {
    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param method XMLRPC method
     * @param params the parameters to append to the request URL
     * @param listener the success listener
     * @param errorListener the error listener
     */
    fun buildGetRequest(
        url: String,
        method: XMLRPC,
        params: List<Any>,
        listener: (Array<Any>) -> Unit,
        errorListener: (BaseNetworkError) -> Unit
    ): XMLRPCRequest {
        return XMLRPCRequest(url, method, params, listener, errorListener)
    }

    /**
     * Creates a new GET request.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param method XMLRPC method
     * @param params the parameters to append to the request URL
     */
    suspend fun syncGetRequest(
        restClient: BaseXMLRPCClient,
        url: String,
        method: XMLRPC,
        params: List<String>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false
    ) = suspendCancellableCoroutine<Response> { cont ->
        val request = buildGetRequest(url, method, params, {
            cont.resume(Success(it))
        }, {
            cont.resume(Error(it))
        })
        cont.invokeOnCancellation { request.cancel() }
        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }
        if (forced) {
            request.setShouldForceUpdate()
        }
        restClient.add(request)
    }

    sealed class Response {
        data class Success(val data: Array<Any>) : Response()
        data class Error(val error: BaseNetworkError) : Response()
    }
}

