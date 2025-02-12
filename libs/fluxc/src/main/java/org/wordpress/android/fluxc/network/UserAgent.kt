package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.util.PackageUtils

class UserAgent(appContext: Context, appName: String) {
    var userAgent: String

    init {
        // default to using just the app name and version
        val appWithVersion = "$appName/${PackageUtils.getVersionName(appContext)}"
        userAgent = appWithVersion

        // then get the default user agent on a separate thread - this can cause an ANR on the main thread
        // see: https://github.com/wordpress-mobile/WordPress-Android/issues/21679#issuecomment-2654510229
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val defaultUserAgent = WebSettings.getDefaultUserAgent(appContext)
                if (defaultUserAgent.isNotEmpty()) {
                    userAgent = "$defaultUserAgent $appWithVersion"
                }
            } catch (e: RuntimeException) {
                // `getDefaultUserAgent()` can throw an Exception
                // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
            }
        }
    }

    override fun toString(): String = userAgent
}
