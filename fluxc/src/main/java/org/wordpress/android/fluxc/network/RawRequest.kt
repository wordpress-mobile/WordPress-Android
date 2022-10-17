package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser

/**
 * A request that allows returning the network response untouched, this is useful when
 * we want to read the network response headers.
 */
class RawRequest(
    method: Int,
    url: String,
    private val listener: Listener<NetworkResponse>,
    onErrorListener: BaseErrorListener
) : BaseRequest<NetworkResponse>(method, url, onErrorListener) {
    override fun parseNetworkResponse(networkResponse: NetworkResponse): Response<NetworkResponse> {
        return Response.success(
            networkResponse,
            HttpHeaderParser.parseCacheHeaders(networkResponse)
        )
    }

    override fun deliverResponse(networkResponse: NetworkResponse) {
        listener.onResponse(networkResponse)
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError = error
}
