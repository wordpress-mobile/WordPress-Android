package org.wordpress.android.ui.domains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContract
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewClient.DomainRegistrationCheckoutWebViewClientListener
import org.wordpress.android.util.SiteUtils

class DomainRegistrationCheckoutWebViewActivity : WPWebViewActivity(), DomainRegistrationCheckoutWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun createWebViewClient(allowedURL: List<String>?) = DomainRegistrationCheckoutWebViewClient(this)

    override fun onCheckoutSuccess() {
        setResult(RESULT_OK, intent)
        finish()
    }

    class OpenCheckout : ActivityResultContract<CheckoutDetails, DomainRegistrationCompletedEvent?>() {
        override fun createIntent(context: Context, input: CheckoutDetails) =
            Intent(context, DomainRegistrationCheckoutWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(URL_TO_LOAD, getCheckoutUrl(input.site))
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(REGISTRATION_DOMAIN_NAME, input.domainName)
                putExtra(REGISTRATION_EMAIL, input.site.email)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): DomainRegistrationCompletedEvent? {
            val data = intent?.takeIf { it.hasExtra(REGISTRATION_DOMAIN_NAME) && it.hasExtra(REGISTRATION_EMAIL) }
            if (resultCode == RESULT_OK && data != null) {
                val domainName = data.getStringExtra(REGISTRATION_DOMAIN_NAME).orEmpty()
                val email = data.getStringExtra(REGISTRATION_EMAIL).orEmpty()
                return DomainRegistrationCompletedEvent(domainName, email)
            }
            return null
        }

        data class CheckoutDetails(val site: SiteModel, val domainName: String)

        private fun getCheckoutUrl(site: SiteModel) =
            "https://wordpress.com/checkout/${SiteUtils.getHomeURLOrHostName(site)}"

        companion object {
            const val REGISTRATION_DOMAIN_NAME = "REGISTRATION_DOMAIN_NAME"
            const val REGISTRATION_EMAIL = "REGISTRATION_EMAIL"
        }
    }
}
