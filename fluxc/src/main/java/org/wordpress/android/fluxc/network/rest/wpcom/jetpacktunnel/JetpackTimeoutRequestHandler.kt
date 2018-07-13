package org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel

import com.android.volley.Response.Listener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.util.AppLog
import java.lang.reflect.Type

/**
 * Wraps a [WPComGsonRequest] with a custom error handler for Jetpack timeout errors (which occur when the overall
 * request to the Jetpack site takes longer than 5 seconds).
 *
 * Will retry up to [maxRetries] times, and finally trigger the normal error handler for the request.
 */
class JetpackTimeoutRequestHandler<T>(
    url: String,
    params: Map<String, String>,
    type: Type,
    listener: Listener<JetpackTunnelResponse<T>>,
    errorListener: WPComErrorListener,
    retryListener: (WPComGsonRequest<*>) -> Unit,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    private val gsonRequest: WPComGsonRequest<JetpackTunnelResponse<T>>
    private var numRetries = 0

    init {
        val wrappedErrorListener = buildJPTimeoutRetryListener(url, errorListener, retryListener)
        gsonRequest = WPComGsonRequest.buildGetRequest(url, params, type, listener, wrappedErrorListener)
    }

    companion object {
        const val DEFAULT_MAX_RETRIES = 2
    }

    fun getRequest(): WPComGsonRequest<JetpackTunnelResponse<T>> {
        return gsonRequest
    }

    /**
     * Wraps the given [WPComErrorListener] in a new one that recognizes Jetpack timeout errors and triggers the
     * [jpTimeoutListener] (if provided) to do custom handling.
     */
    private fun buildJPTimeoutRetryListener(
        wpApiEndpoint: String,
        wpComErrorListener: WPComErrorListener,
        jpTimeoutListener: (WPComGsonRequest<JetpackTunnelResponse<T>>) -> Unit
    ): WPComErrorListener {
        return WPComErrorListener { error ->
            if (error.apiError == "http_request_failed" && error.message.startsWith("cURL error 28")) {
                if (numRetries < maxRetries) {
                    AppLog.e(AppLog.T.API, "5-second timeout reached for endpoint $wpApiEndpoint, retrying...")
                    numRetries++
                    jpTimeoutListener(gsonRequest)
                } else {
                    AppLog.e(AppLog.T.API,
                            "5-second timeout reached for endpoint $wpApiEndpoint - maximum retries reached")
                    wpComErrorListener.onErrorResponse(error)
                }
            } else {
                wpComErrorListener.onErrorResponse(error)
            }
        }
    }
}
