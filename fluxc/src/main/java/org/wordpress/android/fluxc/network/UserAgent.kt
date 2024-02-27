package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import org.wordpress.android.util.PackageUtils

@SuppressWarnings("SwallowedException", "TooGenericExceptionCaught", "MemberNameEqualsClassName")
class UserAgent(appContext: Context?, appName: String) {
    val userAgent: String

    init {
        // Device's default User-Agent string.
        // E.g.:
        //   "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //   AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36"
        val defaultUserAgent = try {
            WebSettings.getDefaultUserAgent(appContext)
        } catch (e: RuntimeException) {
            // `getDefaultUserAgent()` can throw an Exception
            // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
            ""
        }
        // User-Agent string when making HTTP connections, for both API traffic and WebViews.
        // Appends "wp-android/version" to WebView's default User-Agent string for the webservers
        // to get the full feature list of the browser and serve content accordingly, e.g.:
        //    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36
        //    wp-android/4.7"
        val appWithVersion = "$appName/${PackageUtils.getVersionName(appContext)}"
        userAgent = if (defaultUserAgent.isNotEmpty()) "$defaultUserAgent $appWithVersion" else appWithVersion
    }

    override fun toString(): String = userAgent
}
