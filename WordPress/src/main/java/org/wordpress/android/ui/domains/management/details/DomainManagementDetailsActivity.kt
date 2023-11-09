package org.wordpress.android.ui.domains.management.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity

class DomainManagementDetailsActivity : WPWebViewActivity(),
    DomainManagementDetailsWebViewClient.DomainManagementWebViewClientListener {
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
        fun createIntent(context: Context, domainDetailUrl: String): Intent =
            Intent(context, DomainManagementDetailsActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, domainDetailUrl)
            }
    }
}
