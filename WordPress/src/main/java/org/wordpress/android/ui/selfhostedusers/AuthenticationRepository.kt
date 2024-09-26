package org.wordpress.android.ui.selfhostedusers

import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.wpAuthenticationFromUsernameAndPassword
import javax.inject.Inject

class AuthenticationRepository
@Inject constructor(
) {
    private val authenticatedSites = mutableMapOf<AuthenticatedSite, WpAuthentication>()

    private val localTestSiteUrl = "FIXME"
    private val localTestSiteUsername = "FIXME"
    private val localTestSitePassword = "FIXME"

    init {
        addAuthenticatedSite(
            localTestSiteUrl,
            localTestSiteUsername,
            localTestSitePassword
        )
    }

    private fun addAuthenticatedSite(siteUrl: String, username: String, password: String): Boolean {
        if (siteUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            authenticatedSites[AuthenticatedSite(name = siteUrl, url = ParsedUrl.parse(siteUrl))] =
                wpAuthenticationFromUsernameAndPassword(username, password)
            return true
        }
        return false
    }

    fun authenticatedSiteList(): List<AuthenticatedSite> =
        authenticatedSites.keys.toList().sortedBy { it.name }

    fun authenticationForSite(authenticatedSite: AuthenticatedSite): WpAuthentication? =
        authenticatedSites[authenticatedSite]
}
