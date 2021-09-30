package org.wordpress.android.ui.domains

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebViewClient
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.domains.DomainRegistrationWebViewClient.DomainRegistrationWebViewClientListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.DOMAIN_REGISTRATION
import org.wordpress.android.util.SiteUtils

class DomainRegistrationWebViewActivity : WPWebViewActivity(), DomainRegistrationWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
    }

    override fun createWebViewClient(allowedURL: List<String>?): WebViewClient {
        return DomainRegistrationWebViewClient(this, this)
    }

    override fun onRegistrationSuccess() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val DOMAIN_REGISTRATION_REQUEST_CODE = 100123

        fun openCheckout(activity: Activity, site: SiteModel) {
            val checkoutUrl = getCheckoutUrl(site)

            AppLog.d(DOMAIN_REGISTRATION, "Opening checkout URL: $checkoutUrl")

            if (!checkContextAndUrl(activity, checkoutUrl)) {
                return
            }

            val intent = Intent(activity, DomainRegistrationWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(URL_TO_LOAD, checkoutUrl)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
            }

            activity.startActivityForResult(intent, DOMAIN_REGISTRATION_REQUEST_CODE)
        }

        private fun getCheckoutUrl(site: SiteModel) =
                "https://wordpress.com/checkout/${SiteUtils.getHomeURLOrHostName(site)}"
    }
}
