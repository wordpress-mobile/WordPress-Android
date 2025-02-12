package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.util.PackageUtils

@SuppressWarnings("SwallowedException", "TooGenericExceptionCaught")
class UserAgent(appContext: Context, appName: String) {
    var userAgent: String

    init {
        // default to using just the app name and version
        val appWithVersion = "$appName/${PackageUtils.getVersionName(appContext)}"
        userAgent = appWithVersion

        // then get the default user agent on a separate thread - this can cause an ANR on the main thread
        // see: https://github.com/wordpress-mobile/WordPress-Android/issues/21679#issuecomment-2654510229
        CoroutineScope(Dispatchers.Default).launch {
            getDefaultUserAgent(appContext)?.let {
                userAgent = "$it $appWithVersion"
            }
        }
    }

    private suspend fun getDefaultUserAgent(appContext: Context): String? {
        withContext(Dispatchers.IO) {
            try {
                return@withContext WebSettings.getDefaultUserAgent(appContext)
            } catch (e: RuntimeException) {
                // `getDefaultUserAgent()` can throw an Exception
                // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
            }
        }
        return null
    }

    override fun toString(): String = userAgent
}
