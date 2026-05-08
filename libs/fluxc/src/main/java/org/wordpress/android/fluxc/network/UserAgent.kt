package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PackageUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UserAgent @JvmOverloads constructor(
    private val appContext: Context,
    private val appName: String,
    private val executor: Executor = SINGLE_THREAD_EXECUTOR
) {
    private val appVersionName by lazy {
        "$appName/${PackageUtils.getVersionName(appContext)}"
    }

    // Eagerly compute the system http.agent string on a background thread.
    // First read of this property can trigger webkit initialization on some
    // devices, which has caused ANRs during Application.onCreate().
    private val systemHttpAgentFuture: CompletableFuture<String> =
        CompletableFuture.supplyAsync(
            { System.getProperty("http.agent") ?: "" },
            executor
        )

    // Eagerly compute the default WebView user agent on a background thread
    // to avoid blocking the main thread on WebView JNI initialization.
    private val defaultWebViewUserAgentFuture: CompletableFuture<String> =
        CompletableFuture.supplyAsync(
            {
                runCatching {
                    WebSettings.getDefaultUserAgent(appContext)
                }.onFailure {
                    // `getDefaultUserAgent()` can throw an Exception
                    // see: https://github.com/wordpress-mobile/WordPress-Android/issues/20147#issuecomment-1961238187
                    AppLog.e(AppLog.T.UTILS, "Error getting default user agent", it)
                }.getOrNull().orEmpty()
            },
            executor
        )

    /**
     * User-Agent string when making API requests.
     * See https://github.com/woocommerce/woocommerce-android/pull/14431/
     */
    val apiUserAgent: String by lazy {
        val systemUserAgent = systemHttpAgentFuture.get()
        "$systemUserAgent $appVersionName".trim()
    }

    /**
     * User-Agent string to be used in WebView.
     */
    val webViewUserAgent: String by lazy {
        val systemUserAgent = defaultWebViewUserAgentFuture.get()
        "$systemUserAgent $appVersionName".trim()
    }

    companion object {
        private val SINGLE_THREAD_EXECUTOR =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "user-agent-init").apply {
                    isDaemon = true
                }
            }
    }
}
