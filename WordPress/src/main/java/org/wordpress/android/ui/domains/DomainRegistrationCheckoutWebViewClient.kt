package org.wordpress.android.ui.domains

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import org.wordpress.android.util.ErrorManagedWebViewClient

class DomainRegistrationCheckoutWebViewClient(
    private val listener: DomainRegistrationCheckoutWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    private var hasRegistered = false

    interface DomainRegistrationCheckoutWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onCheckoutSuccess()
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val host = request.url.host.orEmpty()
        val path = request.url.path.orEmpty()

        // TODO Revisit this logic
        if (host == WPCOM_API_HOST) {
            if (hasRegistered) {
                listener.onCheckoutSuccess()
            } else if (path == "/rest/v1.1/me/transactions") {
                hasRegistered = true
            }
        }

        return super.shouldInterceptRequest(view, request)
    }

    companion object {
        private const val WPCOM_API_HOST = "public-api.wordpress.com"
    }
}
