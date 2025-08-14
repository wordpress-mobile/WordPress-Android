package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpComSiteId
import javax.inject.Inject

class JetpackConnector @Inject constructor(
    private val jetpackConnectionHelper: JetpackConnectionHelper,
) {
    /**
     * Connects the Jetpack site to WordPress.com and returns the site ID
     */
    suspend fun connectSite(site: SiteModel): Result<WpComSiteId> = runCatching {
        val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
        val wpComSiteId = client.connectSite(CONNECT_FROM)
        requireValidSiteId(wpComSiteId)
    }

    /**
     * Connects the Jetpack user to WordPress.com and returns the site ID
     */
    suspend fun connectUser(site: SiteModel, accessToken: String): Result<WpComSiteId> = runCatching {
        val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
        val wpComAuthentication = WpAuthentication.Bearer(token = accessToken)
        val wpComSiteId = client.connectUser(
            wpComAuthentication = wpComAuthentication,
            from = CONNECT_FROM
        )
        requireValidSiteId(wpComSiteId)
    }

    private fun requireValidSiteId(wpComSiteId: WpComSiteId): WpComSiteId {
        require(wpComSiteId > 0UL) { "Jetpack connection failed, no site ID returned" }
        return wpComSiteId
    }

    companion object {
        const val CONNECT_FROM = "jetpack-android-app"
    }
}
