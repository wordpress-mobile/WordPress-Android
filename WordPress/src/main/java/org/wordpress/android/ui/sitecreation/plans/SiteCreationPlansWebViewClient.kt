package org.wordpress.android.ui.sitecreation.plans

import android.net.Uri
import android.os.Parcelable
import android.webkit.WebResourceRequest
import android.webkit.WebView
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewNavigationDelegate
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewNavigationDelegate.toUrl
import org.wordpress.android.util.ErrorManagedWebViewClient

class SiteCreationPlansWebViewClient(
    private val listener: SiteCreationPlansWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    private val navigationDelegate = DomainRegistrationCheckoutWebViewNavigationDelegate

    interface SiteCreationPlansWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onPlanSelected(url: String)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        if (canNavigateTo(request.url)) return false
        if (request.isRedirect) {
            listener.onPlanSelected(request.url.toString())
        }
        return true
    }

    private fun canNavigateTo(uri: Uri) = navigationDelegate.canNavigateTo(uri.toUrl())
}

@Parcelize
data class PlanModel(
    val productId: Int?,
    val productSlug: String?,
    val productName: String?,
    val isCurrentPlan: Boolean,
    val hasDomainCredit: Boolean
) : Parcelable
