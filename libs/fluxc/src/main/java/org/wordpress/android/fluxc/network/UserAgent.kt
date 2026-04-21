package org.wordpress.android.fluxc.network

import android.content.Context
import android.webkit.WebSettings
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.PackageUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UserAgent @JvmOverloads constructor(
    private val appContext: Context,
    private val appName: String,
    defaultUserAgentExecutor: Executor = SINGLE_THREAD_EXECUTOR
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

    // Eagerly compute the default WebView user agent on a background
    // thread to avoid blocking the main thread with WebView JNI init.
    private val defaultWebViewUserAgentFuture: CompletableFuture<String> =
        CompletableFuture.supplyAsync(
            {
                runCatching {
                    WebSettings.getDefaultUserAgent(appContext)
                }.onFailure {
                    AppLog.e(
                        AppLog.T.UTILS,
                        "Error getting default user agent",
                        it
                    )
                }.getOrNull().orEmpty()
            },
            defaultUserAgentExecutor
        )

    /**
     * User-Agent string to be used in WebView.
     */
    val webViewUserAgent: String by lazy {
        val systemUserAgent = try {
            defaultWebViewUserAgentFuture.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            AppLog.e(AppLog.T.UTILS, "Interrupted while getting default user agent", e)
            ""
        } catch (e: ExecutionException) {
            AppLog.e(AppLog.T.UTILS, "Error getting default user agent", e)
            ""
        }
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
