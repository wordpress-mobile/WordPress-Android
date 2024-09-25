package org.wordpress.android.ui.selfhostedusers

import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.wpAuthenticationFromUsernameAndPassword

class AuthenticationRepository(
    localTestSiteUrl: String,
    localTestSiteUsername: String,
    localTestSitePassword: String
) {
    private val authenticatedSites = mutableMapOf<AuthenticatedSite, WpAuthentication>()

    init {
        addAuthenticatedSite(localTestSiteUrl, localTestSiteUsername, localTestSitePassword)
    }

    fun addAuthenticatedSite(siteUrl: String, username: String, password: String): Boolean {
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
