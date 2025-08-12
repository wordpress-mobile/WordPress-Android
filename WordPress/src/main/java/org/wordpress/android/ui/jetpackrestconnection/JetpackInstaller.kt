package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginWpOrgDirectorySlug
import uniffi.wp_api.PluginsRequestExecutor
import javax.inject.Inject

/**
 * Installs the Jetpack plugin on the given site using wordpress-rs
 */
class JetpackInstaller @Inject constructor(
    private val appLogWrapper: AppLogWrapper,
)  {
    @Inject
    private lateinit var requestExecutor: PluginsRequestExecutor

    suspend fun installJetpack(site: SiteModel): PluginStatus {
        if (site.isJetpackInstalled) {
            appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack is already installed")
            return if (site.isJetpackConnected) {
                PluginStatus.ACTIVE
            } else {
                PluginStatus.INACTIVE
            }
        }

        val params = PluginCreateParams(
            slug = PluginWpOrgDirectorySlug(PLUGIN_SLUG),
            status = PluginStatus.ACTIVE,
        )
        val response = requestExecutor.create(params)
        appLogWrapper.d(AppLog.T.API, "$TAG: Jetpack install response = $response")
        return response.data.status
    }

    companion object {
        private const val TAG = "JetpackInstaller"
        private const val PLUGIN_SLUG = "jetpack"
    }
}
