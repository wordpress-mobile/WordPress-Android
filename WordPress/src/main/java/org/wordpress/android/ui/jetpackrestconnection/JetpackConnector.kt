package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel

class JetpackConnector {
    suspend fun connectSite(site: SiteModel): Result<Boolean> {
        return Result.success(true)
    }
}
