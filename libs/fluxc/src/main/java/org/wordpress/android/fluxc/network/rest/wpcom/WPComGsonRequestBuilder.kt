package org.wordpress.android.fluxc.network.rest.wpcom

import org.wordpress.android.fluxc.network.BaseRequest
import javax.inject.Inject
import javax.inject.Singleton

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
    fun <T> buildGetRequest(url: String,
                            params: Map<String, String>,
                            clazz: Class<T>,
                            listener: (T) -> Unit,
                            errorListener: (BaseRequest.BaseNetworkError) -> Unit): WPComGsonRequest<T> {
        return WPComGsonRequest.buildGetRequest(url, params, clazz, listener, errorListener)
    }
}
