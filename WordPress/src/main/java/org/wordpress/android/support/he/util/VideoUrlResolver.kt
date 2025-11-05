package org.wordpress.android.support.he.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Helper class to resolve video URLs that may have redirect chains.
 * This is particularly useful for Zendesk attachment URLs which use multiple redirects
 * with authentication tokens.
 */
class VideoUrlResolver @Inject constructor() {
    companion object {
        private const val TAG = "VideoUrlResolver"
    }

    /**
     * Resolves a video URL by following all redirects and returning the final URL.
     *
     * @param url The original video URL
     * @return The final URL after following all redirects, or the original URL if resolution fails
     */
    suspend fun resolveUrl(url: String): String = withContext(Dispatchers.IO) {
        try {
            var redirectCount = 0
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .addNetworkInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (response.isRedirect) {
                        redirectCount++
                        Log.d(TAG, "Redirect #$redirectCount: ${request.url} -> ${response.header("Location")}")
                    }
                    response
                }
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0") // Request only first byte to minimize data transfer
                .build()

            Log.d(TAG, "Resolving URL: $url")

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()

                when {
                    response.isSuccessful || response.code == 206 -> {
                        Log.d(TAG, "Successfully resolved URL after $redirectCount redirects: $finalUrl")
                        finalUrl
                    }
                    finalUrl != url -> {
                        // Even if response isn't successful, use the final URL if it's different
                        Log.w(TAG, "Using final URL despite response code ${response.code}: $finalUrl")
                        finalUrl
                    }
                    else -> {
                        Log.e(TAG, "Failed to resolve URL. Response code: ${response.code}")
                        url
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URL: $url", e)
            url
        }
    }
}
