package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

interface ApplicationPasswordsUnavailableListener {
    fun featureIsUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError)
}
