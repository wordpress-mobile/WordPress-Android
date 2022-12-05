package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel

interface ApplicationPasswordsUnavailableListener {
    fun featureIsUnavailable(siteModel: SiteModel)
}
