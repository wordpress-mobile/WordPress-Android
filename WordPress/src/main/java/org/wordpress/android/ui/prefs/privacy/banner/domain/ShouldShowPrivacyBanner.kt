package org.wordpress.android.ui.prefs.privacy.banner.domain

import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import javax.inject.Inject

class ShouldShowPrivacyBanner @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val isUsersCountryGdprCompliant: IsUsersCountryGdprCompliant,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
) {
    operator fun invoke(): Boolean {
        return fluxCUtilsWrapper.isSignedInWPComOrHasWPOrgSite()
                && !appPrefs.savedPrivacyBannerSettings
                && isUsersCountryGdprCompliant()
    }
}
