package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError

interface ApplicationPasswordsListener {
    fun onNewPasswordCreated(isPasswordRegenerated: Boolean) {}
    fun onPasswordGenerationFailed(networkError: WPAPINetworkError) {}
    fun onFeatureUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError) {}

    // Hardware-backed Keystore failure (e.g. Tink AndroidKeystoreAesGcm InvalidKeyException)
    // when reading or writing the encrypted credentials store. Implementations should treat
    // this as a non-fatal so an influx of failures stays visible in crash reporting.
    fun onKeystoreError(error: Throwable) {}
}
