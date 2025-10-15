package org.wordpress.android.networking

import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Request.Priority
import com.bumptech.glide.integration.volley.VolleyRequestFactory
import com.bumptech.glide.integration.volley.VolleyStreamFetcher
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPUrlUtils
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

// Timeout in milliseconds for loading complex images at server side (private, heavy...)
private const val TIMEOUT = 15_000

/**
 * RequestFactory which adds authorization headers to all Glide requests and makes sure requests to WPcom endpoints
 * use https.
 */
@Singleton
class GlideRequestFactory @Inject constructor(
    private val authenticationUtils: AuthenticationUtils
) : VolleyRequestFactory {
    override fun create(
        url: String,
        callback: DataCallback<in InputStream>,
        priority: Priority,
        headers: Map<String, String>
    ): Request<ByteArray>? {
        val httpsUrl: String = convertWPcomUrlToHttps(url)
        return VolleyStreamFetcher.GlideRequest(
            httpsUrl,
            callback,
            priority,
            addAuthHeaders(url, headers)
        ).apply {
            retryPolicy = DefaultRetryPolicy(
                TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }
    }

    private fun convertWPcomUrlToHttps(url: String): String {
        return if (WPUrlUtils.isWordPressCom(url) && !UrlUtils.isHttps(url)) UrlUtils.makeHttps(url) else url
    }

    private fun addAuthHeaders(url: String, currentHeaders: Map<String, String>): MutableMap<String, String> {
        val authenticationHeaders = authenticationUtils.getAuthHeaders(url)
        val headers = currentHeaders.toMutableMap()
        authenticationHeaders.entries.forEach { (key, value) ->
            headers[key] = value
        }
        return headers
    }
}
