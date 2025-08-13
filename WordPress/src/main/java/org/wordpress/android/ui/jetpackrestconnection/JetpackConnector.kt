package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackrestconnection.JetpackConnectionHelper.Companion.CONNECT_FROM
import javax.inject.Inject

class JetpackConnector @Inject constructor(
    private val jetpackConnectionHelper: JetpackConnectionHelper
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun connectSite(site: SiteModel): Result<ULong> {
        try {
            val client = jetpackConnectionHelper.initJetpackConnectionClient(site)
            val wpComSiteId = client.connectSite(CONNECT_FROM)
            return if (wpComSiteId > 0u) {
                return Result.success(wpComSiteId)
            } else (
                Result.failure(
                    Exception("Jetpack connection failed, no site ID returned")
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
