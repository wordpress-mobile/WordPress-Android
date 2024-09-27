package org.wordpress.android.fluxc.network.discovery

import com.android.volley.Header
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
    errorListener: BaseErrorListener,
    private val mListener: Listener<String?>
) : BaseRequest<List<Header>?>(Method.HEAD, url, errorListener) {
    override fun deliverResponse(response: List<Header>?) {
        val endpoint = response?.firstNotNullOfOrNull { extractEndpointFromLinkHeader(it.value) }
        mListener.onResponse(endpoint)
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<List<Header>?>? {
        val headers = response.allHeaders
            ?.filter { it.name.equals(LINK_HEADER_NAME, ignoreCase = true) }
            ?.flatMap {
                it.value.split(",")
                    .map { value -> Header(LINK_HEADER_NAME, value.trimStart()) }
            }
            ?.ifEmpty { null }

        return if (headers != null) {
            Response.success(headers, HttpHeaderParser.parseCacheHeaders(response))
        } else {
            Response.error(ParseError(Exception("No headers in response")))
        }
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError {
        // no op
        return WPAPINetworkError(error, null)
    }

    companion object {
        private const val LINK_HEADER_NAME = "Link"
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
