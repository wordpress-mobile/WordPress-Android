package org.wordpress.android.ui.prefs.privacy.banner.domain

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.privacy.GeoRepository
import javax.inject.Inject

class ShouldAskPrivacyConsent @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val geoRepository: GeoRepository,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
) {
    suspend operator fun invoke(): Boolean {
        return isLoggedIn &&
                !appPrefs.savedPrivacyBannerSettings &&
                geoRepository.isGdprComplianceRequired()
    }

    private val isLoggedIn
        get () = accountStore.hasAccessToken() || selectedSiteRepository.getSelectedSite()?.let {
            // If the selected site isn't using WPCOM REST, we assume it's logged in via self-hosted.
            it.origin != SiteModel.ORIGIN_WPCOM_REST
        } ?: false
}
