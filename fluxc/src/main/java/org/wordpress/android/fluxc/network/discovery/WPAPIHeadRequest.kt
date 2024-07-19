package org.wordpress.android.fluxc.network.discovery

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import java.util.regex.Pattern

class WPAPIHeadRequest(
    url: String,
    private val mListener: Listener<String?>,
    errorListener: BaseErrorListener
) : BaseRequest<String?>(Method.HEAD, url, errorListener) {
    private var mResponseLinkHeader: String? = null

    protected override fun deliverResponse(response: String?) {
        mListener.onResponse(extractEndpointFromLinkHeader(mResponseLinkHeader))
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<String?>? {
        val headers = response.headers
        if (headers != null) {
            mResponseLinkHeader = headers["Link"]
            return Response.success("", HttpHeaderParser.parseCacheHeaders(response))
        } else {
            return Response.error(ParseError(Exception("No headers in response")))
        }
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError {
        // no op
        return WPAPINetworkError(error, null)
    }

    companion object {
        private val LINK_PATTERN: Pattern = Pattern.compile("^<(.*)>; rel=\"https://api.w.org/\"$")

        private fun extractEndpointFromLinkHeader(linkHeader: String?): String? {
            if (linkHeader != null) {
                val matcher = LINK_PATTERN.matcher(linkHeader)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            }
            return null
        }
    }
}
