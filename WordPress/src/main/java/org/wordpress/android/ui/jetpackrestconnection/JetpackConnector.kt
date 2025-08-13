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
            return Result.success(wpComSiteId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
