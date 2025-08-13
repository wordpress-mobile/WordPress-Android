package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import rs.wordpress.api.kotlin.WpApiClient
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

class JetpackConnectionHelper @Inject constructor() {
    fun initWpApiClient(site: SiteModel): WpApiClient {
        if (!validateCredentials(site)) {
            throw IllegalArgumentException("Missing credentials for Jetpack WpApiClient")
        }
        return WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                site.apiRestUsernamePlain!!,
                site.apiRestPasswordPlain!!
            )
        )
    }

    private fun validateCredentials(site: SiteModel): Boolean {
        return !site.apiRestUsernamePlain.isNullOrBlank() && !site.apiRestPasswordPlain.isNullOrBlank()
    }
}
