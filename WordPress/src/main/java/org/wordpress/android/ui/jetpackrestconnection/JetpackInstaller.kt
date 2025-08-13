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
    suspend fun installJetpack(site: SiteModel): PluginStatus? {
        // Validate required credentials
        if (site.apiRestUsernamePlain.isNullOrBlank() || site.apiRestPasswordPlain.isNullOrBlank()) {
            appLogWrapper.e(AppLog.T.API, "$TAG: Missing credentials for Jetpack installation")
            return null
        }

        val info = getPluginInfo(site)
        return when (info?.status) {
            PluginStatus.ACTIVE, PluginStatus.NETWORK_ACTIVE -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed and activated")
                PluginStatus.ACTIVE
            }

            PluginStatus.INACTIVE -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is installed but inactive")
                val newStatus = if (info.isNetworkOnly) {
                    PluginStatus.NETWORK_ACTIVE
                } else {
                    PluginStatus.ACTIVE
                }
                activatePlugin(site, newStatus)
            }

            else -> {
                appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is not installed")
                createPlugin(site)
            }
        }
    }

    /**
     * Creates the Jetpack plugin on the given site using wordpress-rs, returns null on failure
     */
    private suspend fun createPlugin(site: SiteModel): PluginStatus? {
        val response = createApiClient(site).request { requestBuilder ->
            requestBuilder.plugins().create(
                PluginCreateParams(
                    slug = JETPACK_SLUG_WPORG_DIRECTORY,
                    status = PluginStatus.ACTIVE
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                response.response.data.status
            }

            is WpRequestResult.WpError<*> -> {
                appLogWrapper.e(AppLog.T.API, "$TAG: Installation failed - ${response.errorCode}")
                null
            }

            else -> {
                appLogWrapper.e(AppLog.T.API, "$TAG: Installation failed")
                null
            }
        }
    }

    /**
     * Activates the Jetpack plugin on the given site using wordpress-rs, returns null on failure
     */
    private suspend fun activatePlugin(
        site: SiteModel,
        newStatus: PluginStatus
    ): PluginStatus? {
        val params = PluginUpdateParams(
            status = newStatus
        )
        val response = createApiClient(site).request { requestBuilder ->
            requestBuilder.plugins().update(
                pluginSlug = JETPACK_SLUG,
                params = params
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                response.response.data.status
            }

            is WpRequestResult.WpError<*> -> {
                appLogWrapper.e(AppLog.T.API, "$TAG: Activation failed - ${response.errorCode}")
                null
            }

            else -> {
                appLogWrapper.e(AppLog.T.API, "$TAG: Activation failed")
                null
            }
        }
    }

    /**
     * Get the Jetpack plugin info on the given site using wordpress-rs, returns null if not installed
     */
    private suspend fun getPluginInfo(site: SiteModel): PluginInfo? {
        val response = createApiClient(site).request { requestBuilder ->
            requestBuilder.plugins().listWithEditContext(
                params = PluginListParams(
                    search = JETPACK_SLUG.slug
                )
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                val plugins = response.response.data
                plugins.firstOrNull {
                    it.plugin.slug == JETPACK_SLUG.slug
                }?.let { jetpack ->
                    PluginInfo(
                        name = jetpack.name,
                        status = jetpack.status,
                        isNetworkOnly = jetpack.networkOnly,
                        version = jetpack.version
                    )
                } ?: run {
                    appLogWrapper.d(AppLog.T.API, "$TAG: Plugin info not found")
                    null
                }
            }

            else -> {
                appLogWrapper.e(AppLog.T.API, "$TAG: Plugin info failed")
                null
            }
        }
    }

    private fun createApiClient(site: SiteModel) = WpApiClient(
        wpOrgSiteApiRootUrl = URL(site.wpApiRestUrl),
        authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
            site.apiRestUsernamePlain!!,
            site.apiRestPasswordPlain!!
        )
    )

    @Suppress("Unused")
    private class PluginInfo(
        val name: String,
        val status: PluginStatus,
        val isNetworkOnly: Boolean,
        val version: String
    )

    companion object {
        private const val TAG = "JetpackInstaller"
        private val JETPACK_SLUG = PluginSlug("jetpack/jetpack")
        private val JETPACK_SLUG_WPORG_DIRECTORY = PluginWpOrgDirectorySlug("jetpack")
    }
}
