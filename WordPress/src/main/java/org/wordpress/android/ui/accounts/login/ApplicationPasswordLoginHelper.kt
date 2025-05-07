package org.wordpress.android.ui.accounts.login

import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import javax.inject.Inject

class ApplicationPasswordLoginHelper @Inject constructor(
    private val siteSqlUtils: SiteSqlUtils
) {
    private var processedAppPasswordData: String? = null

    @Suppress("ReturnCount")
    fun storeApplicationPasswordCredentialsFrom(data: String?): Boolean {
        if (data == null || data == processedAppPasswordData) {
            return false
        }

        val siteUrl = data.toUri().getQueryParameter("site_url")
        val user = data.toUri().getQueryParameter("user_login")
        val password = data.toUri().getQueryParameter("password")

        if (user.isNullOrEmpty() || password.isNullOrEmpty() ) {
            return false
        }

        runBlocking {
            val site = siteSqlUtils.getSites().firstOrNull { it.url == siteUrl }
            if (site != null) {
                site.apiRestUsername = user
                site.apiRestPassword = password
                siteSqlUtils.insertOrUpdateSite(site)
                Log.d("WP_RS", "Saved application password credentials for: $siteUrl")
            } else {
                Log.e("WP_RS", "Cannot save application password credentials for: $siteUrl")
            }
        }

        processedAppPasswordData = data
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
}
