package org.wordpress.android.ui.blaze

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WebViewRenderProcessGone

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

    /**
     * The render process is gone, so this WebView cannot show the campaign any more. Report it as
     * a load error so the screen shows its error state, and return true so the framework does not
     * take the app down with the renderer.
     */
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        WebViewRenderProcessGone.log(AppLog.T.MAIN, detail)
        errorReceived = true
        blazeWebViewClientListener.onWebViewReceivedError(requestedUrl)
        return true
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
