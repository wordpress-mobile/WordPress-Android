package org.wordpress.android.ui.domains.management.details

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.wordpress.android.ui.domains.management.details.DomainManagementDetailsWebViewNavigationDelegate.toUrl
import org.wordpress.android.util.ErrorManagedWebViewClient

class DomainManagementDetailsWebViewClient(
    private val listener: DomainManagementWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    interface DomainManagementWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onRedirectToExternalBrowser(url: String)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        if (canNavigateTo(request.url)) return false
        listener.onRedirectToExternalBrowser(request.url.toString())
        return true
    }

    private fun canNavigateTo(uri: Uri) =
        DomainManagementDetailsWebViewNavigationDelegate.canNavigateTo(uri.toUrl())
}
