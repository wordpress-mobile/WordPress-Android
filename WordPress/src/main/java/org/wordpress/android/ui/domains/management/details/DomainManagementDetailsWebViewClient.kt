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

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        if (url != null && url != "about:blank" && !navigationDelegate.canNavigateTo(url)) {
            listener.onRedirectToExternalBrowser(url)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        if (navigationDelegate.canNavigateTo(request.url)) return false
        listener.onRedirectToExternalBrowser(request.url.toString())
        return true
    }
}
