package org.wordpress.android.ui.domains.management

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import org.wordpress.android.ui.WPWebViewActivity

class DomainManagementDetailsActivity : WPWebViewActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    companion object {
        private fun getDomainDetailsUrl(domainName: String) =
            "https://wordpress.com/domains/manage/all/$domainName/edit/$domainName"

        fun createIntent(context: Context, domainName: String): Intent =
            Intent(context, DomainManagementDetailsActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, getDomainDetailsUrl(domainName))
            }
    }
}
