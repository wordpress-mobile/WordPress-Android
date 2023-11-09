package org.wordpress.android.ui.domains.management.details

import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.wordpress.android.util.ErrorManagedWebViewClient

class DomainManagementDetailsWebViewClient(
    private val navigationDelegate: DomainManagementDetailsWebViewNavigationDelegate,
    private val listener: DomainManagementWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    interface DomainManagementWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onRedirectToExternalBrowser(url: String)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        if (navigationDelegate.canNavigateTo(request.url)) return false
        listener.onRedirectToExternalBrowser(request.url.toString())
        return true
    }
}
