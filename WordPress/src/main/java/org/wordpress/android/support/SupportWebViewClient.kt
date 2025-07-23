package org.wordpress.android.support

import android.webkit.WebResourceRequest
import androidx.core.net.toUri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import org.wordpress.android.util.ErrorManagedWebViewClient

class SupportWebViewClient(
    private val listener: SupportWebViewClientListener,
    private val assetLoader: WebViewAssetLoader
) : ErrorManagedWebViewClient(listener) {
    interface SupportWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onSupportTapped(chatHistory: String)
        fun onRedirectToExternalBrowser(url: String)
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    // to support API < 21
    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(
        view: WebView,
        url: String
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(url.toUri())
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        listener.onRedirectToExternalBrowser(request.url.toString())
        return true
    }
}


