package org.wordpress.android.ui.accounts.login

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import javax.inject.Inject

class ApplicationPasswordLoginHelper @Inject constructor(
    private val siteSqlUtils: SiteSqlUtils,
    private val uriLoginWrapper: UriLoginWrapper
) {
    private var processedAppPasswordData: String? = null

    @Suppress("ReturnCount")
    suspend fun storeApplicationPasswordCredentialsFrom(url: String): Boolean {
        if (url.isEmpty() || url == processedAppPasswordData) {
            return false
        }

        // Check if the url can be parsed
        val uriLogin = uriLoginWrapper.parseUriLogin(url)

        if (uriLogin.user.isNullOrEmpty() || uriLogin.password.isNullOrEmpty() ) {
            return false
        }

        runBlocking {
            val site = siteSqlUtils.getSites().firstOrNull { it.url == uriLogin.siteUrl }
            if (site != null) {
                site.apiRestUsername = uriLogin.user
                site.apiRestPassword = uriLogin.password
                siteSqlUtils.insertOrUpdateSite(site)
                Log.d("WP_RS", "Saved application password credentials for: ${uriLogin.siteUrl}")
            } else {
                Log.e("WP_RS", "Cannot save application password credentials for: ${uriLogin.siteUrl}")
            }
        }

        processedAppPasswordData = url
        return true
    }

    fun appendParamsToRestAuthorizationUrl(authorizationUrl: String?): String {
        return if (authorizationUrl.isNullOrEmpty()) {
            authorizationUrl.orEmpty()
        } else {
            // TODO: Add clientID
            authorizationUrl.toUri().buildUpon().apply {
                appendQueryParameter("app_name", "android-jetpack-client")
                appendQueryParameter("success_url", "jetpack://app-pass-authorize")
            }.build().toString()
        }
    }

    /**
     * This class is created to wrap the Uri calls and let us unit test the login helper
     */
    class UriLoginWrapper @Inject constructor() {
        fun parseUriLogin(url: String): UriLogin {
            val uri = url.toUri()
            return UriLogin(
                uri.getQueryParameter("site_url"),
                uri.getQueryParameter("user_login"),
                uri.getQueryParameter("password")
            )
        }
    }

    data class UriLogin(
        val siteUrl: String?,
        val user: String?,
        val password: String?
    )
}
