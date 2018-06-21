package org.wordpress.android.networking

import android.util.Base64
import com.android.volley.Request
import com.android.volley.Request.Priority
import com.bumptech.glide.integration.volley.VolleyRequestFactory
import com.bumptech.glide.integration.volley.VolleyStreamFetcher
import com.bumptech.glide.load.data.DataFetcher.DataCallback
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPUrlUtils
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RequestFactory which adds authorization headers to all Glide requests and makes sure requests to WPcom endpoints
 * use https.
 */
@Singleton
class GlideRequestFactory @Inject constructor(
    private val accessToken: AccessToken,
    private val httpAuthManager: HTTPAuthManager,
    private val userAgent: UserAgent
) : VolleyRequestFactory {
    override fun create(
        url: String,
        callback: DataCallback<in InputStream>,
        priority: Priority,
        headers: Map<String, String>
    ): Request<ByteArray>? {
        val httpsUrl: String = convertWPcomUrlToHttps(url)
        return VolleyStreamFetcher.GlideRequest(httpsUrl, callback, priority, addAuthHeaders(url, headers))
    }

    private fun convertWPcomUrlToHttps(url: String): String {
        return if (WPUrlUtils.isWordPressCom(url) && !UrlUtils.isHttps(url)) UrlUtils.makeHttps(url) else url
    }

    private fun addAuthHeaders(url: String, currentHeaders: Map<String, String>): MutableMap<String, String> {
        val headers = currentHeaders.toMutableMap()
        headers["User-Agent"] = userAgent.userAgent
        if (WPUrlUtils.safeToAddWordPressComAuthToken(url)) {
            if (accessToken.exists()) {
                headers["Authorization"] = "Bearer " + accessToken.get()
            }
        } else {
            // Check if we had HTTP Auth credentials for the root url
            val httpAuthModel = httpAuthManager.getHTTPAuthModel(url)
            if (httpAuthModel != null) {
                val creds = String.format("%s:%s", httpAuthModel.username, httpAuthModel.password)
                val auth = "Basic " + Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
                headers["Authorization"] = auth
            }
        }
        return headers
    }
}
