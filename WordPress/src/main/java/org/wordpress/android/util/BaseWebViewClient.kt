package org.wordpress.android.util

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

open class BaseWebViewClient : WebViewClient() {
    private var mWebResourceError = false
    var mBaseWebViewClientListener: BaseWebViewClientListener? = null

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mWebResourceError = false
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (!mWebResourceError) {
            mBaseWebViewClientListener?.onPageLoaded()
        }
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        mWebResourceError = true
        mBaseWebViewClientListener?.onError()
    }

    interface BaseWebViewClientListener {
        fun onPageLoaded()
        fun onError()
    }
}
