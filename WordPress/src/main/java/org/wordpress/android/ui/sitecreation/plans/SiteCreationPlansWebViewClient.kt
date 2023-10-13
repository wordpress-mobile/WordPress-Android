package org.wordpress.android.ui.sitecreation.plans

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.wordpress.android.util.ErrorManagedWebViewClient

class SiteCreationPlansWebViewClient(
    private val listener: SiteCreationPlansWebViewClientListener
) : ErrorManagedWebViewClient(listener) {
    interface SiteCreationPlansWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onPlanSelected(uri: Uri)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.toString().startsWith(JETPACK_APP_PLANS_PATH)) return false

        val urlString = request.url.toString()
        if (urlString.contains(PLAN_SLUG)) {
            listener.onPlanSelected(request.url)
        }
        return true
    }

    companion object {
        private const val PLAN_SLUG = "plan_slug"
        private const val JETPACK_APP_PLANS_PATH = "https://wordpress.com/jetpack-app/plans"
    }
}
