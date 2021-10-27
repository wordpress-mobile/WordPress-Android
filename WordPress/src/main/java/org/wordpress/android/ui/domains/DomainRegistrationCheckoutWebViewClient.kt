package org.wordpress.android.ui.domains

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.net.toUri
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewNavigationDelegate.toUrl
import org.wordpress.android.util.ErrorManagedWebViewClient

class DomainRegistrationCheckoutWebViewClient(
    private val listener: DomainRegistrationCheckoutWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    private val navigationDelegate = DomainRegistrationCheckoutWebViewNavigationDelegate

    interface DomainRegistrationCheckoutWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onCheckoutSuccess()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = !canNavigateTo(request.url)

    private fun canNavigateTo(uri: Uri) = navigationDelegate.canNavigateTo(uri.toUrl())

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        if (isCheckoutSuccessPage(url.toUri())) {
            listener.onCheckoutSuccess()
        } else {
            super.doUpdateVisitedHistory(view, url, isReload)
        }
    }

    private fun isCheckoutSuccessPage(uri: Uri) = uri.path.orEmpty().startsWith(CHECKOUT_SUCCESS_PATH_PREFIX)

    companion object {
        private const val CHECKOUT_SUCCESS_PATH_PREFIX = "/checkout/thank-you/"
    }
}
