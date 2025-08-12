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
 * Installs the Jetpack plugin on the given site using wordpress-rs if it's not already installed
 */
class JetpackInstaller @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
) {
    suspend fun installJetpack(site: SiteModel): Pair<InstallJetpackStatus, Int> {
        if (site.isJetpackInstalled) {
            return if (site.isJetpackConnected) {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and active")
                Pair(InstallJetpackStatus.ACTIVE, 200)
            } else {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and inactive")
                Pair(InstallJetpackStatus.INACTIVE, 200)
            }
        }

        val wpApiClient = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                requireNotNull(site.apiRestUsernamePlain) { "Username is required but was null" },
                requireNotNull(site.apiRestPasswordPlain) { "Password is required but was null" }
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
        when (response) {
            is WpRequestResult.Success -> {
                return when (response.response.data.status) {
                    PluginStatus.ACTIVE,
                    PluginStatus.NETWORK_ACTIVE -> {
                        Pair(InstallJetpackStatus.ACTIVE, 200)
                    }

                    PluginStatus.INACTIVE -> {
                        Pair(InstallJetpackStatus.INACTIVE, 200)
                    }
                }
            }

            is WpRequestResult.WpError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.errorCode}")
                return Pair(InstallJetpackStatus.FAILED, 0)
            }

            is WpRequestResult.UnknownError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.statusCode}")
                return Pair(InstallJetpackStatus.FAILED, response.statusCode.toInt())
            }

            else -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = unknown")
                return Pair(InstallJetpackStatus.FAILED, 0)
            }
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
