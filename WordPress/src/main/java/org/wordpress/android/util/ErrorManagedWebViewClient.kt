package org.wordpress.android.util

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

open class ErrorManagedWebViewClient(
    private val baseWebViewClientListener: ErrorManagedWebViewClientListener
) : WebViewClient() {
    private var errorReceived = false
    private var requestedUrl: String? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        errorReceived = false
        requestedUrl = url
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (!errorReceived) {
            baseWebViewClientListener.onWebViewPageLoaded()
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        // From the documentation:
        // > These errors usually indicate inability to connect to the server.
        // > will be called for any resource (iframe, image, etc.), not just for the main page.
        // > Thus, it is recommended to perform minimum required work in this callback.
        if (request?.isForMainFrame == true && requestedUrl == request.url.toString()) {
            errorReceived = true
            baseWebViewClientListener.onWebViewReceivedError()
        }
    }

    /**
     * The render process is gone, so this WebView cannot show anything any more. Report it as a
     * load error - every screen using this client already has an error state for that - and
     * return true so the framework does not take the app down with the renderer.
     */
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        WebViewRenderProcessGone.log(AppLog.T.UTILS, detail)
        errorReceived = true
        baseWebViewClientListener.onWebViewReceivedError()
        return true
    }

    interface ErrorManagedWebViewClientListener {
        fun onWebViewPageLoaded()
        fun onWebViewReceivedError()
    }
}
