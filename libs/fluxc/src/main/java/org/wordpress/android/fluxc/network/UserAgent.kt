package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PackageUtils

@Suppress("MemberNameEqualsClassName")
class UserAgent @JvmOverloads constructor(
    private val appContext: Context?,
    private val appName: String,
    bgDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * User-Agent string when making HTTP connections, for both API traffic and WebViews.
     * Appends "[appName]/version" to WebView's default User-Agent string for the webservers
     * to get the full feature list of the browser and serve content accordingly, e.g.:
     *    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
     *    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36
     *    wp-android/4.7"
     */
    var userAgent: String = getAppNameVersion()
        private set

    private val coroutineScope = CoroutineScope(bgDispatcher)

    init {
        coroutineScope.launch {
            initUserAgent()
        }
    }

    /**
     * Initializes the User-Agent string.
     * This method will be called asynchronously to avoid blocking the main thread,
     * because `WebSettings.getDefaultUserAgent()` can be slow.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun initUserAgent() {
        // Device's default User-Agent string.
        // E.g.:
        //   "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //   AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36"
        val defaultUserAgent = try {
            WebSettings.getDefaultUserAgent(appContext)
        } catch (e: RuntimeException) {
            // `getDefaultUserAgent()` can throw an Exception
            // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
            AppLog.e(
                AppLog.T.UTILS,
                "Error getting the user's default User-Agent, ${e.stackTraceToString()}"
            )
            return
        }

        userAgent = "$defaultUserAgent ${getAppNameVersion()}"
    }

    private fun getAppNameVersion() = "$appName/${PackageUtils.getVersionName(appContext)}"

    override fun toString(): String = userAgent
}
