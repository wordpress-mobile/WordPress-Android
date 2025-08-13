package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginListParams
import uniffi.wp_api.PluginSlug
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginUpdateParams
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
    private lateinit var apiClient: WpApiClient

    suspend fun installJetpack(site: SiteModel): PluginStatus? {
        if (!validateCredentials(site)) return null

        initApiClient(site)

        val info = getPluginInfo()
        return when (info?.status) {
            PluginStatus.ACTIVE, PluginStatus.NETWORK_ACTIVE -> {
                logDebug("Jetpack is already installed and activated")
                info.status
            }
            PluginStatus.INACTIVE -> {
                logDebug("Jetpack is installed but inactive")
                val targetStatus = if (info.isNetworkOnly) {
                    PluginStatus.NETWORK_ACTIVE
                } else {
                    PluginStatus.ACTIVE
                }
                activatePlugin(targetStatus)
            }
            null -> {
                logDebug("Jetpack is not installed")
                createAndActivatePlugin()
            }
        }
    }

    private fun validateCredentials(site: SiteModel): Boolean {
        if (site.apiRestUsernamePlain.isNullOrBlank() || site.apiRestPasswordPlain.isNullOrBlank()) {
            logError("Missing credentials for Jetpack installation")
            return false
        }
        return true
    }

    private fun initApiClient(site: SiteModel) {
        apiClient = WpApiClient(
            wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
            authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                site.apiRestUsernamePlain!!,
                site.apiRestPasswordPlain!!
            )
        )
    }

    private suspend fun getPluginInfo(): PluginInfo? {
        val response = apiClient.request { requestBuilder ->
            requestBuilder.plugins().listWithEditContext(
                params = PluginListParams(search = JETPACK_SLUG.slug)
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                response.response.data.firstOrNull {
                    it.plugin.slug == JETPACK_SLUG.slug
                }?.let {
                    PluginInfo(
                        status = it.status,
                        isNetworkOnly = it.networkOnly
                    )
                }
            }
            else -> {
                logError("Failed to get plugin info")
                null
            }
        }
    }

    private suspend fun activatePlugin(targetStatus: PluginStatus): PluginStatus? {
        logDebug("Activating Jetpack plugin with status: $targetStatus")

        val response = apiClient.request { requestBuilder ->
            requestBuilder.plugins().update(
                pluginSlug = JETPACK_SLUG,
                params = PluginUpdateParams(status = targetStatus)
            )
        }

        return when (response) {
            is WpRequestResult.Success -> response.response.data.status
            is WpRequestResult.WpError<*> -> {
                logError("Activation failed - ${response.errorCode}")
                null
            }
            else -> {
                logError("Activation failed")
                null
            }
        }
    }

    private suspend fun createAndActivatePlugin(): PluginStatus? {
        logDebug("Installing and activating Jetpack plugin")

        val response = apiClient.request { requestBuilder ->
            requestBuilder.plugins().create(
                PluginCreateParams(
                    slug = JETPACK_SLUG_WPORG_DIRECTORY,
                    status = PluginStatus.ACTIVE
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                logDebug("Installation successful")
                response.response.data.status
            }
            is WpRequestResult.WpError<*> -> {
                logError("Installation failed - ${response.errorCode}")
                null
            }
            else -> {
                logError("Installation failed")
                null
            }
        }
    }

    private fun logDebug(message: String) {
        appLogWrapper.d(AppLog.T.API, "$TAG: $message")
    }

    private fun logError(message: String) {
        appLogWrapper.e(AppLog.T.API, "$TAG: $message")
    }

    private data class PluginInfo(
        val status: PluginStatus,
        val isNetworkOnly: Boolean
    )

    companion object {
        private const val TAG = "JetpackInstaller"
        private val JETPACK_SLUG = PluginSlug("jetpack/jetpack")
        private val JETPACK_SLUG_WPORG_DIRECTORY = PluginWpOrgDirectorySlug("jetpack")
    }
}
