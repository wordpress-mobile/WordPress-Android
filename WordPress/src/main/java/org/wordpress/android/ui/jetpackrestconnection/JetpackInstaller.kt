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
                return when (response.successfulResponse()?.data?.status) {
                    PluginStatus.ACTIVE -> {
                        InstallJetpackStatus.ACTIVE
                    }

                    PluginStatus.INACTIVE -> {
                        InstallJetpackStatus.INACTIVE
                    }
                    // TODO not sure what NETWORK_ACTIVE means
                    PluginStatus.NETWORK_ACTIVE -> {
                        InstallJetpackStatus.ACTIVE
                    }

                    null -> {
                        InstallJetpackStatus.FAILED
                    }
                }
            }

            is WpRequestResult.WpError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.errorCode}")
                return InstallJetpackStatus.FAILED
            }

            is WpRequestResult.UnknownError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.statusCode}")
                return InstallJetpackStatus.FAILED
            }

            else -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = unknown")
                return InstallJetpackStatus.FAILED
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
