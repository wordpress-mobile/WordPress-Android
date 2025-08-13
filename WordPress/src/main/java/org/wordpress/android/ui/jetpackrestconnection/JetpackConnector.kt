package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackrestconnection.JetpackConnectionHelper.Companion.CONNECT_FROM
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpComSiteId
import javax.inject.Inject

class JetpackConnector @Inject constructor(
    private val jetpackConnectionHelper: JetpackConnectionHelper
) {
    /**
     * Connects the Jetpack site to WordPress.com and returns the site ID
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun connectSite(site: SiteModel): Result<WpComSiteId> {
        try {
            val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
            val wpComSiteId = client.connectSite(CONNECT_FROM)
            return checkWpComIdResult(wpComSiteId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Connects the Jetpack user to WordPress.com and returns the site ID
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun connectUser(site: SiteModel, accessToken: String): Result<WpComSiteId> {
        try {
            val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
            val wpComAuthentication = WpAuthentication.Bearer(token = accessToken)
            val wpComSiteId = client.connectUser(
                wpComAuthentication = wpComAuthentication,
                from = CONNECT_FROM
            )
            return checkWpComIdResult(wpComSiteId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun checkWpComIdResult(wpComSiteId: WpComSiteId): Result<WpComSiteId> {
        return if (wpComSiteId > 0UL) {
            return Result.success(wpComSiteId)
        } else {
            Result.failure(
                Exception("Jetpacks connection failed, no site ID returned")
            )
        }
    }
}
