package org.wordpress.android.fluxc.network.rest.wpcom

import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class WPComGsonRequestBuilder
@Inject constructor() {
    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    fun <T> buildGetRequest(
        url: String,
        params: Map<String, String>,
        clazz: Class<T>,
        listener: (T) -> Unit,
        errorListener: (WPComGsonNetworkError) -> Unit
    ): WPComGsonRequest<T> {
        return WPComGsonRequest.buildGetRequest(url, params, clazz, listener, errorListener)
    }

    /**
     * Creates a new GET request.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     */
    suspend fun <T> syncGetRequest(
        restClient: BaseWPComRestClient,
        url: String,
        params: Map<String, String>,
        clazz: Class<T>
    ) = suspendCoroutine<Response<T>> {cont ->
        restClient.add(WPComGsonRequest.buildGetRequest(url, params, clazz, {
            cont.resume(Success(it))
        }, {
            cont.resume(Error(it))
        }))
    }

    /**
     * Creates a new JSON-formatted POST request.
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    fun <T> buildPostRequest(
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>,
        listener: (T) -> Unit,
        errorListener: (WPComGsonNetworkError) -> Unit
    ): WPComGsonRequest<T> {
        return WPComGsonRequest.buildPostRequest(url, body, clazz, listener, errorListener)
    }

    /**
     * Creates a new JSON-formatted POST request, triggers it and awaits results synchronously.
     * @param restClient rest client that handles the request
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
     * @param clazz the class defining the expected response
     */
    suspend fun <T> syncPostRequest(
        restClient: BaseWPComRestClient,
        url: String,
        body: Map<String, Any>,
        clazz: Class<T>
    ) = suspendCoroutine<Response<T>> { cont ->
        restClient.add(WPComGsonRequest.buildPostRequest(url, body, clazz, {
            cont.resume(Success(it))
        }, {
            cont.resume(Error(it))
        }))
    }

    sealed class Response<T> {
        data class Success<T>(val data: T) : Response<T>()
        data class Error<T>(val error: WPComGsonNetworkError) : Response<T>()
    }
}
