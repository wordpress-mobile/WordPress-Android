package org.wordpress.android.networking

import android.util.Base64

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.LazyHeaders.Builder
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader

import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.utils.WPUrlUtils

import java.io.InputStream

import javax.inject.Inject

/**
 * Glide Header loader which adds an authorization header and a user agent header to requests.
 */
class GlideHeaderLoader @Inject constructor(
    modelLoader: ModelLoader<GlideUrl, InputStream>,
    private val mAccessToken: AccessToken,
    private val mHttpAuthManager: HTTPAuthManager,
    private val mUserAgent: UserAgent) : BaseGlideUrlLoader<GlideUrl>(modelLoader) {
    override fun getHeaders(url: GlideUrl?, width: Int, height: Int, options: Options?): Headers? {
        var headerBuilder = Builder()

        url?.let {
            headerBuilder = headerBuilder.addHeader("User-Agent", mUserAgent.userAgent)
            if (WPUrlUtils.safeToAddWordPressComAuthToken(url.toStringUrl())) {
                headerBuilder = headerBuilder.addHeader("Authorization", "Bearer " + mAccessToken.get())
            } else {
                // Check if we had HTTP Auth credentials for the root url
                val httpAuthModel = mHttpAuthManager.getHTTPAuthModel(url.toStringUrl())
                if (httpAuthModel != null) {
                    val creds = String.format("%s:%s", httpAuthModel.username, httpAuthModel.password)
                    val auth = "Basic " + Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
                    headerBuilder.addHeader("Authorization", auth)
                }
            }
        }
        return headerBuilder.build()
    }

    override fun getUrl(s: GlideUrl, width: Int, height: Int, options: Options): String {
        return s.toStringUrl()
    }

    override fun handles(s: GlideUrl): Boolean {
        return true
    }
}
