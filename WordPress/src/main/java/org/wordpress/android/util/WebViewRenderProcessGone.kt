package org.wordpress.android.util

import android.webkit.RenderProcessGoneDetail

/**
 * Shared handling for `WebViewClient.onRenderProcessGone`.
 *
 * A WebView's render process is torn down either because it crashed or because the system
 * reclaimed its memory. Whichever it was, that WebView is unusable afterwards and the callback
 * must return `true`, otherwise the framework takes the whole app process down with it. Clients
 * log the termination here and then fall back to the error state their screen already shows.
 */
object WebViewRenderProcessGone {
    @JvmStatic
    fun log(tag: AppLog.T, detail: RenderProcessGoneDetail?) {
        val reason = if (detail?.didCrash() == true) {
            "it crashed"
        } else {
            "the system reclaimed its memory"
        }
        AppLog.e(tag, "WebView render process is gone because $reason")
    }
}
