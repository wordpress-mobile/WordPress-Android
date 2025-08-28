package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PackageUtils

class UserAgent(
    private val appContext: Context,
    private val appName: String
) {
    private val appVersionName by lazy {
        "$appName/${PackageUtils.getVersionName(appContext)}"
    }

    /**
     * User-Agent string when making API requests.
     * See https://github.com/woocommerce/woocommerce-android/pull/14431/
     */
    val apiUserAgent: String by lazy {
        val systemUserAgent = System.getProperty("http.agent") ?: ""
        "$systemUserAgent $appVersionName".trim()
    }

    /**
     * User-Agent string to be used in WebView.
     */
    val webViewUserAgent: String by lazy {
        val systemUserAgent = runCatching {
            WebSettings.getDefaultUserAgent(appContext)
        }.onFailure {
            // `getDefaultUserAgent()` can throw an Exception
            // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
            AppLog.e(AppLog.T.UTILS, "Error getting default user agent", it)
        }.getOrNull().orEmpty()

        "$systemUserAgent $appVersionName".trim()
    }
}
