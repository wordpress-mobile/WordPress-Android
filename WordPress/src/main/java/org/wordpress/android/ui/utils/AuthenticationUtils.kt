package org.wordpress.android.ui.utils

import android.util.Base64
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.util.WPUrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationUtils
@Inject constructor(
    private val accessToken: AccessToken,
    private val httpAuthManager: HTTPAuthManager,
    private val userAgent: UserAgent,
    private val privateAtomicCookie: PrivateAtomicCookie
) {
    fun getAuthHeaders(url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["User-Agent"] = userAgent.userAgent

        // add cookie header to Aztec media requests on private Atomic sites
        if (privateAtomicCookie.exists() && WPUrlUtils.safeToAddPrivateAtCookie(url, privateAtomicCookie.getDomain())) {
            headers[COOKIE_HEADER_NAME] = privateAtomicCookie.getCookieContent()
        }

        if (WPUrlUtils.safeToAddWordPressComAuthToken(url)) {
            if (accessToken.exists()) {
                headers[AUTHORIZATION_HEADER_NAME] = "Bearer " + accessToken.get()
            }
        } else {
            // Check if we had HTTP Auth credentials for the root url
            val httpAuthModel = httpAuthManager.getHTTPAuthModel(url)
            if (httpAuthModel != null) {
                val creds = String.format("%s:%s", httpAuthModel.username, httpAuthModel.password)
                val auth = "Basic " + Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
                headers[AUTHORIZATION_HEADER_NAME] = auth
            }
        }
        return headers
    }

    companion object {
        const val AUTHORIZATION_HEADER_NAME = "Authorization"
        const val COOKIE_HEADER_NAME = "Cookie"
    }
}
