package org.wordpress.android.ui.domains.management.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity

class DomainManagementDetailsActivity : WPWebViewActivity(),
    DomainManagementDetailsWebViewClient.DomainManagementWebViewClientListener {
    private val domainArg: String get() = intent.getStringExtra(PICKED_DOMAIN_KEY) ?: error("Domain cannot be null.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun createWebViewClient(allowedURL: List<String>?) = DomainManagementDetailsWebViewClient(this)

    override fun onRedirectToExternalBrowser(url: String) {
        ActivityLauncher.openUrlExternal(this, url)
    }

    companion object {
        const val PICKED_DOMAIN_KEY: String = "picked_domain_key"

        fun createIntent(context: Context, domain: String, domainDetailUrl: String): Intent =
            Intent(context, DomainManagementDetailsActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, domainDetailUrl)
                putExtra(PICKED_DOMAIN_KEY, domain)
            }
    }
}
