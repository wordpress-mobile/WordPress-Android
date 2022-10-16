package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser

class RawRequest(
    method: Int,
    url: String,
    private val listener: Listener<NetworkResponse>,
    onErrorListener: BaseErrorListener
) : BaseRequest<NetworkResponse>(method, url, onErrorListener) {
    override fun parseNetworkResponse(p0: NetworkResponse): Response<NetworkResponse> {
        return Response.success(p0, HttpHeaderParser.parseCacheHeaders(p0))
    }

    override fun deliverResponse(p0: NetworkResponse) {
        listener.onResponse(p0)
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError = error
}
