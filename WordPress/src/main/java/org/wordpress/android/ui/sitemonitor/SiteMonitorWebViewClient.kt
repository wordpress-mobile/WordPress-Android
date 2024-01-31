package org.wordpress.android.ui.sitemonitor

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class SiteMonitorWebViewClient(
    private val listener: SiteMonitorWebViewClientListener,
    private val tabType: SiteMonitorType
) : WebViewClient() {
    private var errorReceived = false
    private var requestedUrl: String? = null

    interface SiteMonitorWebViewClientListener {
        fun onWebViewPageLoaded(url: String, tabType: SiteMonitorType)
        fun onWebViewReceivedError(url: String, tabType: SiteMonitorType)
    }
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        errorReceived = false
        requestedUrl = url
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (!errorReceived) {
            url?.let { listener.onWebViewPageLoaded(it, tabType) }
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
            listener.onWebViewReceivedError(request.url.toString(), tabType)
        }
    }
}
