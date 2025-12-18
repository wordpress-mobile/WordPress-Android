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
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.util.AppLog
import org.wordpress.gutenberg.RecordedNetworkRequest

/**
 * Logs GutenbergKit WebView network requests to Chucker by replaying them through OkHttp.
 *
 * GutenbergKit intercepts JavaScript `fetch` calls and reports them via [RecordedNetworkRequest].
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
    fun log(networkRequest: RecordedNetworkRequest) {
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
            .tag(RecordedNetworkRequest::class.java, networkRequest)
            .build()

        @Suppress("TooGenericExceptionCaught")
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            // Catch all exceptions since various things can fail (IOException, IllegalStateException, etc.)
            // and we don't want logging failures to crash the app
            AppLog.e(AppLog.T.EDITOR, "Failed to log GutenbergKit network request", e)
        }
    }
}

/**
 * Interceptor that returns a pre-recorded response from the request's tag.
 * Used to "replay" GutenbergKit requests through OkHttp without making actual network calls.
 */
private class ReplayInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val networkRequest = chain.request().tag(RecordedNetworkRequest::class.java)
            ?: error("ReplayInterceptor requires RecordedNetworkRequest tag")

        val contentType = networkRequest.responseHeaders["content-type"]?.toMediaTypeOrNull()

        val responseBody = networkRequest.responseBody?.toResponseBody(contentType)
            ?: "".toResponseBody(contentType)

        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(networkRequest.status)
            .message(networkRequest.statusText)
            .headers(networkRequest.responseHeaders.toHeaders())
            .body(responseBody)
            .build()
    }
}
