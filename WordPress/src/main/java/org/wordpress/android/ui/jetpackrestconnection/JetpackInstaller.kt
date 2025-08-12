package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginWpOrgDirectorySlug
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

/**
 * Installs the Jetpack plugin on the given site using wordpress-rs if it's not already installed
 */
class JetpackInstaller @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
) {
    suspend fun installJetpack(site: SiteModel): InstallJetpackStatus {
        if (site.isJetpackInstalled) {
            return if (site.isJetpackConnected) {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and active")
                InstallJetpackStatus.ACTIVE
            } else {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and inactive")
                InstallJetpackStatus.INACTIVE
            }
        }

        val wpApiClient = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.url),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                requireNotNull(site.username) { "Username is required but was null" },
                requireNotNull(site.password) { "Password is required but was null" }
            ),
        )

        val params = PluginCreateParams(
            slug = PluginWpOrgDirectorySlug(PLUGIN_SLUG),
            status = PluginStatus.ACTIVE,
        )
        val response = wpApiClient.request { requestBuilder ->
            requestBuilder.plugins().create(
                params = params
            )
        }

        appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install response = $response")
        return when (response.successfulResponse()?.data?.status) {
            PluginStatus.ACTIVE -> {
                InstallJetpackStatus.ACTIVE
            }
            PluginStatus.INACTIVE -> {
                InstallJetpackStatus.INACTIVE
            }
            // TODO not sure what NETWORK_ACTIVE means
            PluginStatus.NETWORK_ACTIVE -> InstallJetpackStatus.FAILED
            null -> InstallJetpackStatus.FAILED
        }
    }

    enum class InstallJetpackStatus {
        ACTIVE,
        INACTIVE,
        FAILED,
    }

    companion object {
        private const val TAG = "JetpackInstaller"
        private const val PLUGIN_SLUG = "jetpack"
    }
}
