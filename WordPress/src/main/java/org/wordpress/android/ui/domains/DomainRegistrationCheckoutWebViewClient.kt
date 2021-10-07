package org.wordpress.android.ui.domains

import org.wordpress.android.util.ErrorManagedWebViewClient

class DomainRegistrationCheckoutWebViewClient(
    private val listener: DomainRegistrationCheckoutWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    interface DomainRegistrationCheckoutWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onCheckoutSuccess()
    }
}
