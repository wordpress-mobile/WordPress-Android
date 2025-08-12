package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginSlug
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginUpdateParams
import uniffi.wp_api.PluginWpOrgDirectorySlug
import uniffi.wp_api.SparsePluginFieldWithEditContext
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
        val wpApiClient = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                requireNotNull(site.apiRestUsernamePlain) { "Username is required but was null" },
                requireNotNull(site.apiRestPasswordPlain) { "Password is required but was null" }
            ),
        )

        // Check if the Jetpack plugin is already installed and whether it's active
        fetchJetpackPluginInfo(wpApiClient)?.let { pluginInfo ->
            when (pluginInfo.status) {
                PluginStatus.ACTIVE,
                PluginStatus.NETWORK_ACTIVE -> {
                    appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and active")
                    return InstallJetpackResult(InstallJetpackStatus.ACTIVE, HTTP_SUCCESS)
                }

                PluginStatus.INACTIVE -> {
                    appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed but inactive")
                    return activateJetpackPlugin(wpApiClient)
                }
            }
        }

        // Jetpack plugin isn't installed so install it now
        return createJetpackPlugin(wpApiClient)
    }

    /**
     * Called when the Jetpack plugin doesn't already exist to install it
     */
    private suspend fun createJetpackPlugin(wpApiClient: WpApiClient): InstallJetpackResult {
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

    /**
     * Called when the Jetpack plugin already exists but is inactive to activate it
     */
    private suspend fun activateJetpackPlugin(wpApiClient: WpApiClient): InstallJetpackResult {
        val params = PluginUpdateParams(
            status = PluginStatus.ACTIVE,
        )
        val response = wpApiClient.request { requestBuilder ->
            requestBuilder.plugins().update(
                pluginSlug = PluginSlug(PLUGIN_SLUG),
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

    /**
     * Fetch the status and version of the Jetpack plugin, returns null if not installed
     */
    private suspend fun fetchJetpackPluginInfo(wpApiClient: WpApiClient): JetpackPluginInfo? {
        val response = wpApiClient.request { requestBuilder ->
            requestBuilder.plugins().filterRetrieveWithEditContext(
                pluginSlug = PluginSlug(PLUGIN_SLUG),
                fields = listOf(
                    SparsePluginFieldWithEditContext.STATUS,
                    SparsePluginFieldWithEditContext.VERSION
                )
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val status = response.response.data.status
                val version = response.response.data.version
                if (status != null && version != null) {
                    JetpackPluginInfo(
                        status = status,
                        version = version,
                    )
                } else {
                    null
                }
            }

            else -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack fetchPluginStatus error = $response")
                null
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

    private data class JetpackPluginInfo(
        val status: PluginStatus,
        val version: String,
    )

    companion object {
        private const val TAG = "JetpackInstaller"
        private const val PLUGIN_SLUG = "jetpack"
        private const val HTTP_SUCCESS: UShort = 200u
    }
}
