package org.wordpress.android.ui.posts.editor

import okhttp3.Headers.Companion.toHeaders
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import android.util.Log
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.gutenberg.NetworkRequest

private const val TAG = "GutenbergKitNetworkLogger"

/**
 * Logs GutenbergKit WebView network requests to Chucker by replaying them through OkHttp.
 *
 * GutenbergKit intercepts JavaScript `fetch` calls and reports them via [NetworkRequest].
 * Since Chucker only captures OkHttp traffic, we "replay" these requests through an OkHttp
 * client with Chucker attached, allowing all network activity to appear in a unified log.
 *
 * Note: This class is provided via [org.wordpress.android.modules.TrackNetworkRequestsModule].
 */
class GutenbergKitNetworkLogger(
    private val trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor
) {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(trackNetworkRequestsInterceptor)
            .addInterceptor(ReplayInterceptor())
            .build()
    }

    /**
     * Logs a GutenbergKit network request to Chucker.
     * Call this from [org.wordpress.gutenberg.GutenbergView.NetworkRequestListener.onNetworkRequest].
     */
    fun log(networkRequest: NetworkRequest) {
        Log.d(TAG, "Logging request: ${networkRequest.method} ${networkRequest.url} -> ${networkRequest.status}")

        val contentType = networkRequest.requestHeaders["content-type"]?.toMediaTypeOrNull()
        // OkHttp doesn't allow request bodies for GET/HEAD methods
        val requestBody = if (networkRequest.method.uppercase() in listOf("GET", "HEAD")) {
            null
        } else {
            networkRequest.requestBody?.toRequestBody(contentType)
        }

        val request = Request.Builder()
            .url(networkRequest.url)
            .method(networkRequest.method, requestBody)
            .headers(networkRequest.requestHeaders.toHeaders())
            .tag(NetworkRequest::class.java, networkRequest)
            .build()

        try {
            client.newCall(request).execute().close()
            Log.d(TAG, "Successfully logged request to Chucker")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log request: ${e.message}", e)
        }
    }
}

/**
 * Interceptor that returns a pre-recorded response from the request's tag.
 * Used to "replay" GutenbergKit requests through OkHttp without making actual network calls.
 */
private class ReplayInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val networkRequest = chain.request().tag(NetworkRequest::class.java)
            ?: error("ReplayInterceptor requires NetworkRequest tag")

        val contentType = networkRequest.responseHeaders["content-type"]?.toMediaTypeOrNull()

        val responseBody = networkRequest.responseBody?.toResponseBody(contentType)
            ?: "".toResponseBody(contentType)

        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(networkRequest.status)
            .message(networkRequest.status.toStatusMessage())
            .headers(networkRequest.responseHeaders.toHeaders())
            .body(responseBody)
            .build()
    }

    private fun Int.toStatusMessage(): String = when (this) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        301 -> "Moved Permanently"
        302 -> "Found"
        304 -> "Not Modified"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        else -> "Unknown"
    }
}
