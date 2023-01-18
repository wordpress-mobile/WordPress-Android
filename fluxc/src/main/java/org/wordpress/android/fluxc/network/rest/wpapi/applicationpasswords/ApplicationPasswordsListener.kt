package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

interface ApplicationPasswordsListener {
    fun onNewPasswordCreated(isPasswordRegenerated: Boolean) {}
    fun onPasswordGenerationFailed(networkError: WPAPINetworkError) {}
    fun onFeatureUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError) {}
}
