package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackrestconnection.JetpackConnectionHelper.Companion.CONNECT_FROM
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpComSiteId
import javax.inject.Inject

class JetpackConnector @Inject constructor(
    private val jetpackConnectionHelper: JetpackConnectionHelper
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun connectSite(site: SiteModel): Result<WpComSiteId> {
        try {
            val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
            val wpComSiteId = client.connectSite(CONNECT_FROM)
            return if (wpComSiteId > 0u) {
                return Result.success(wpComSiteId)
            } else {
                Result.failure(
                    Exception("Jetpacks site connection failed, no site ID returned")
                )
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun connectUser(site: SiteModel, accessToken: String): Result<WpComSiteId> {
        try {
            val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
            val wpComAuthentication = WpAuthentication.Bearer(token = accessToken)
            val wpComSiteId = client.connectUser(
                wpComAuthentication = wpComAuthentication,
                from = CONNECT_FROM
            )
            return if (wpComSiteId > 0u) {
                return Result.success(wpComSiteId)
            } else {
                Result.failure(
                    Exception("Jetpack user connection failed, no site ID returned")
                )
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
