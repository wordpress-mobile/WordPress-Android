package org.wordpress.android.support.he.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val SUCCESSFULLY_RESOLVED_CODE = 206
private const val TIMEOUT_SECONDS = 30L

/**
 * Helper class to resolve video URLs that may have redirect chains.
 * This is particularly useful for Zendesk attachment URLs which use multiple redirects
 * with authentication tokens.
 */
class VideoUrlResolver @Inject constructor(
    private val appLogWrapper: AppLogWrapper
) {
    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    /**
     * Resolves a video URL by following all redirects and returning the final URL.
     *
     * @param url The original video URL
     * @return The final URL after following all redirects, or the original URL if resolution fails
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun resolveUrl(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0") // Request only first byte to minimize data transfer
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()

                when {
                    response.isSuccessful || response.code == SUCCESSFULLY_RESOLVED_CODE -> {
                        finalUrl
                    }
                    finalUrl != url -> {
                        // Even if response isn't successful, use the final URL if it's different
                        finalUrl
                    }
                    else -> {
                        url
                    }
                }
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.UTILS, "Error resolving support url: ${e.stackTraceToString()}")
            url
        }
    }
}
