package org.wordpress.android.ui.domains

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity.OpenPlans.PlanDetails
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewClient.DomainRegistrationCheckoutWebViewClientListener
import org.wordpress.android.util.SiteUtils

class DomainRegistrationCheckoutWebViewActivity : WPWebViewActivity(), DomainRegistrationCheckoutWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
        setupNavigationButton()
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

    private fun cancelCheckout() {
        mViewModel.track(Stat.WEBVIEW_DISMISSED)
        onCheckoutSuccess()
    }

    private fun setupNavigationButton() {
        if (intent.hasExtra(OpenCheckout.SHOW_CLOSE_BUTTON) &&
            intent.getBooleanExtra(OpenCheckout.SHOW_CLOSE_BUTTON, false)
        ) {
            // Update the back icon with the close icon
            findViewById<Toolbar>(R.id.toolbar).apply {
                setNavigationIcon(R.drawable.ic_close_white_24dp)
                setNavigationOnClickListener { cancelCheckout() }
            }

            onBackPressedDispatcher.addCallback(this) { cancelCheckout() }
        }
    }

    class OpenPlans : ActivityResultContract<PlanDetails, DomainRegistrationCompletedEvent?>() {
        override fun createIntent(context: Context, input: PlanDetails) =
            Intent(context, DomainRegistrationCheckoutWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(URL_TO_LOAD, getPlansUrl(input.site))
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

        data class PlanDetails(val site: SiteModel, val domainName: String)

        private fun getPlansUrl(site: SiteModel) =
            "https://wordpress.com/plans/yearly/" +
                    SiteUtils.getHomeURLOrHostName(site) +
                    "?domainAndPlanPackage=true" +
                    "&jetpackAppPlans=true"

        companion object {
            const val REGISTRATION_DOMAIN_NAME = "REGISTRATION_DOMAIN_NAME"
            const val REGISTRATION_EMAIL = "REGISTRATION_EMAIL"
        }
    }

    class OpenCheckout : ActivityResultContract<CheckoutDetails, DomainRegistrationCompletedEvent?>() {
        override fun createIntent(context: Context, input: CheckoutDetails) =
            Intent(context, DomainRegistrationCheckoutWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(URL_TO_LOAD, getCheckoutUrl(input.site))
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(REGISTRATION_DOMAIN_NAME, input.domainName)
                putExtra(REGISTRATION_EMAIL, input.site.email)
                putExtra(SHOW_CLOSE_BUTTON, input.showCloseButton)
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

        data class CheckoutDetails(val site: SiteModel, val domainName: String, val showCloseButton: Boolean = false)

        private fun getCheckoutUrl(site: SiteModel) =
            "https://wordpress.com/checkout/${SiteUtils.getHomeURLOrHostName(site)}"

        companion object {
            const val REGISTRATION_DOMAIN_NAME = "REGISTRATION_DOMAIN_NAME"
            const val REGISTRATION_EMAIL = "REGISTRATION_EMAIL"
            const val SHOW_CLOSE_BUTTON = "SHOW_CLOSE_BUTTON"
        }
    }
}
