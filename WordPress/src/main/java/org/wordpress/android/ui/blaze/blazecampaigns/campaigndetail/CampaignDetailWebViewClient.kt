package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailWebViewNavigationDelegate.toUrl

import org.wordpress.android.util.ErrorManagedWebViewClient

class CampaignDetailWebViewClient(
    private val listener: CampaignDetailWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    private val navigationDelegate = CampaignDetailWebViewNavigationDelegate

    interface CampaignDetailWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onRedirectToExternalBrowser(url: String)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) : Boolean {
        if (canNavigateTo(request.url)) return false
        listener.onRedirectToExternalBrowser(request.url.toString())
        return true
    }

    private fun canNavigateTo(uri: Uri) = navigationDelegate.canNavigateTo(uri.toUrl())
}
