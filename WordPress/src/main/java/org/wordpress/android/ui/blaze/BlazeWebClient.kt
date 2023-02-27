package org.wordpress.android.ui.blaze

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class BlazeWebViewClient(
    private val blazeWebViewClientListener: OnBlazeWebViewClientListener
) : WebViewClient() {
    private var errorReceived = false
    private var requestedUrl: String? = null

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (!errorReceived) {
            blazeWebViewClientListener.onWebViewPageLoaded(url)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        errorReceived = false
        requestedUrl = url
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        // From the documentation:
        // > These errors usually indicate inability to connect to the server.
        // > will be called for any resource (iframe, image, etc.), not just for the main page.
        // > Thus, it is recommended to perform minimum required work in this callback.
        if (request?.isForMainFrame == true && requestedUrl == request.url.toString()) {
            errorReceived = true
            blazeWebViewClientListener.onWebViewReceivedError()
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        errorReceived = true
        blazeWebViewClientListener.onWebViewReceivedError()
    }

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (url.startsWith(WORDPRESS_ADVERTISING_PATH))
            return false

        blazeWebViewClientListener.onRedirectToExternalBrowser(url)
        return true
    }

    companion object {
        private const val WORDPRESS_ADVERTISING_PATH = "https://wordpress.com/advertising/"
    }
}


interface OnBlazeWebViewClientListener {
    fun onWebViewPageLoaded(url: String)
    fun onWebViewReceivedError()
    fun onRedirectToExternalBrowser(url: String)
}
