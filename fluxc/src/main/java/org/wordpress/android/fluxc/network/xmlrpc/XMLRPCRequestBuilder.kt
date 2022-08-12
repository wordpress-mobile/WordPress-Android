package org.wordpress.android.fluxc.network.xmlrpc

import com.android.volley.Response.Listener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest.XmlRpcErrorType
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequestBuilder.Response.Success
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class XMLRPCRequestBuilder @Inject constructor() {
    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param method XMLRPC method
     * @param params the parameters to append to the request URL
     * @param listener the success listener
     * @param errorListener the error listener
     */
    @Suppress("LongParameterList", "SwallowedException")
    fun <T> buildGetRequest(
        url: String,
        method: XMLRPC,
        params: List<Any>,
        clazz: Class<T>,
        listener: (T) -> Unit,
        errorListener: (BaseNetworkError) -> Unit
    ): XMLRPCRequest {
        return XMLRPCRequest(
            url,
            method,
            params,
            // **Do not** convert it to lambda! See https://youtrack.jetbrains.com/issue/KT-51868
            @Suppress("RedundantSamConstructor")
            Listener<Any> { obj: Any? ->
                if (obj == null) {
                    errorListener.invoke(BaseNetworkError(INVALID_RESPONSE))
                }
                try {
                    clazz.cast(obj)?.let { listener(it) }
                } catch (e: ClassCastException) {
                    errorListener.invoke(
                        BaseNetworkError(
                            INVALID_RESPONSE,
                            XmlRpcErrorType.UNABLE_TO_READ_SITE
                        )
                    )
                }
            },
            errorListener
        )
    }

    /**
     * Creates a new GET request.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param method XMLRPC method
     * @param params the parameters to append to the request URL
     */
    suspend fun <T> syncGetRequest(
        restClient: BaseXMLRPCClient,
        url: String,
        method: XMLRPC,
        params: List<Any>,
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        forced: Boolean = false
    ) = suspendCancellableCoroutine<Response<T>> { cont ->
        val request = buildGetRequest(url, method, params, clazz, {
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

    sealed class Response<T> {
        data class Success<T>(val data: T) : Response<T>()
        data class Error<T>(val error: BaseNetworkError) : Response<T>()
    }
}
