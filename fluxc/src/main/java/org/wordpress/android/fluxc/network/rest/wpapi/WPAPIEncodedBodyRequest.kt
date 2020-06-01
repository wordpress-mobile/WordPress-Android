package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser
import org.wordpress.android.fluxc.network.BaseRequest
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.Charset

class WPAPIEncodedBodyRequest(
    method: Int,
    url: String,
    private val params: Map<String, String>,
    private val body: Map<String, String>,
    private val listener: Listener<String>,
    errorListener: BaseErrorListener
) : BaseRequest<String>(method, url, errorListener) {
    override fun getBody(): ByteArray {
        return encodeParameters(body)
    }

    override fun getParams(): MutableMap<String, String> {
        return params.toMutableMap()
    }

    override fun deliverBaseNetworkError(error: BaseNetworkError): BaseNetworkError {
        return error
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
        val contentTypeCharset =
                response?.headers
                        ?.let { charset(HttpHeaderParser.parseCharset(it)) }
                        ?: Charset.defaultCharset()

        val data = response?.data?.toString(contentTypeCharset)
        return Response.success(data, null)
    }

    override fun deliverResponse(response: String) {
        listener.onResponse(response)
    }

    /**
     * Based on [com.android.volley.Request.encodeParameters]
     * @param params parameters that are converted
     * @return an application/x-www-form-urlencoded encoded string
     */
    private fun encodeParameters(params: Map<String, String>): ByteArray {
        val encodedParams = StringBuilder()
        return try {
            for ((key, value) in params) {
                encodedParams.append(URLEncoder.encode(key, paramsEncoding))
                encodedParams.append('=')
                encodedParams.append(URLEncoder.encode(value, paramsEncoding))
                encodedParams.append('&')
            }
            encodedParams.toString().toByteArray(charset(paramsEncoding))
        } catch (uee: UnsupportedEncodingException) {
            throw RuntimeException("Encoding not supported: $paramsEncoding", uee)
        }
    }
}
