package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginWpOrgDirectorySlug
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

/**
 * Installs the Jetpack plugin on the given site using wordpress-rs
 */
class JetpackInstaller @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
) {
    suspend fun installJetpack(site: SiteModel): InstallJetpackStatus {
        // Skip installation if Jetpack is already active
        if (site.isJetpackInstalled && site.isJetpackConnected) {
            appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and connected")
            return InstallJetpackStatus.ACTIVE
        }

        // Validate required credentials
        if (site.apiRestUsernamePlain.isNullOrBlank() || site.apiRestPasswordPlain.isNullOrBlank()) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Missing credentials for Jetpack installation")
            return InstallJetpackStatus.FAILED
        }

        return try {
            val response = createApiClient(site).request { requestBuilder ->
                requestBuilder.plugins().create(
                    PluginCreateParams(
                        slug = PluginWpOrgDirectorySlug(PLUGIN_SLUG),
                        status = PluginStatus.ACTIVE
                    )
                )
            }

            when (response) {
                is WpRequestResult.Success -> mapPluginStatus(response.response.data.status)
                is WpRequestResult.WpError<*> -> {
                    appLogWrapper.e(AppLog.T.API, "$TAG: Installation failed - ${response.errorCode}")
                    InstallJetpackStatus.FAILED
                }
                else -> {
                    appLogWrapper.e(AppLog.T.API, "$TAG: Installation failed")
                    InstallJetpackStatus.FAILED
                }
            }
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Installation exception - ${e.message}")
            InstallJetpackStatus.FAILED
        }
    }

    private fun createApiClient(site: SiteModel) = WpApiClient(
        wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
        authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            site.apiRestUsernamePlain!!,
            site.apiRestPasswordPlain!!
        )
    )

    private fun mapPluginStatus(status: PluginStatus) = when (status) {
        PluginStatus.ACTIVE, PluginStatus.NETWORK_ACTIVE -> InstallJetpackStatus.ACTIVE
        PluginStatus.INACTIVE -> InstallJetpackStatus.INACTIVE
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
