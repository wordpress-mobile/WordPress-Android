package org.wordpress.android.ui.blaze

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.wordpress.android.util.UriWrapper

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
            blazeWebViewClientListener.onWebViewReceivedError(request.url.toString())
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.let {
            val uri = UriWrapper(it.url)
            return if (uri.toString().startsWith(WORDPRESS_ADVERTISING_PATH)) {
                false
            } else {
                blazeWebViewClientListener.onRedirectToExternalBrowser(uri.toString())
                true
            }
        }
        return false
    }

    companion object {
        private const val WORDPRESS_ADVERTISING_PATH = "https://wordpress.com/advertising/"
    }
}

interface OnBlazeWebViewClientListener {
    fun onWebViewPageLoaded(url: String)
    fun onWebViewReceivedError(url: String?)
    fun onRedirectToExternalBrowser(url: String)
}
