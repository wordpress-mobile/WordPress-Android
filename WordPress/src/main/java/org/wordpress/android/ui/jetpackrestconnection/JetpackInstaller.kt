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
    suspend fun installJetpack(site: SiteModel): InstallJetpackResult {
        if (site.isJetpackInstalled && site.isJetpackConnected) {
            appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and connected")
            return InstallJetpackResult(InstallJetpackStatus.ACTIVE, HTTP_SUCCESS)
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
        when (response) {
            is WpRequestResult.Success -> {
                return when (response.response.data.status) {
                    PluginStatus.ACTIVE,
                    PluginStatus.NETWORK_ACTIVE -> {
                        InstallJetpackResult(InstallJetpackStatus.ACTIVE, HTTP_SUCCESS)
                    }

                    PluginStatus.INACTIVE -> {
                        InstallJetpackResult(InstallJetpackStatus.INACTIVE, HTTP_SUCCESS)
                    }
                }
            }

            is WpRequestResult.WpError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.statusCode}")
                return InstallJetpackResult(InstallJetpackStatus.FAILED, response.statusCode)
            }

            is WpRequestResult.UnknownError<*> -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = ${response.statusCode}")
                return InstallJetpackResult(InstallJetpackStatus.FAILED, response.statusCode)
            }

            else -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install error = $response")
                return InstallJetpackResult(InstallJetpackStatus.FAILED, 0u)
            }
        }
    }

    data class InstallJetpackResult(
        val status: InstallJetpackStatus,
        val httpStatusCode: UShort
    )

    enum class InstallJetpackStatus {
        ACTIVE,
        INACTIVE,
        FAILED,
    }

    companion object {
        private const val TAG = "JetpackInstaller"
        private const val PLUGIN_SLUG = "jetpack"
        private const val HTTP_SUCCESS: UShort = 200u
    }
}
