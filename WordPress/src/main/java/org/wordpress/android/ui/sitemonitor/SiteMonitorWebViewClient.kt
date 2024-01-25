package org.wordpress.android.ui.sitemonitor

import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.wordpress.android.util.ErrorManagedWebViewClient

class SiteMonitorWebViewClient(
    listener: SiteMonitorWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    interface SiteMonitorWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onRedirectToExternalBrowser(url: String)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        return false
    }
}
