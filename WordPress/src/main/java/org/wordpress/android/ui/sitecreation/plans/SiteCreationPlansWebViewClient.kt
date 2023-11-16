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
        if (request.url.toString().startsWith(JETPACK_APP_PLANS_PATH)) {
            val planSlug = request.url.getQueryParameter(PLAN_SLUG).orEmpty()
            if (planSlug.isNotBlank()) {
                listener.onPlanSelected(request.url)
            }
            return false
        }

        return true
    }

    companion object {
        private const val PLAN_SLUG = "plan_slug"
        private const val JETPACK_APP_PLANS_PATH = "https://wordpress.com/jetpack-app/plans"
    }
}
