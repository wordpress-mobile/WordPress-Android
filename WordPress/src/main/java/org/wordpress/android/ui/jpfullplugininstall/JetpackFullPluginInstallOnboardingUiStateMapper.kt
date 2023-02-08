package org.wordpress.android.ui.jpfullplugininstall

import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class JetpackFullPluginInstallOnboardingUiStateMapper @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
) {
    fun mapLoaded(
        onTermsAndConditionsClick: () -> Unit,
        onInstallFullPluginClick: () -> Unit,
        onContactSupportClick: () -> Unit,
    ): UiState.Loaded =
        UiState.Loaded(
            siteName =,
            pluginName =,
            onTermsAndConditionsClick = onTermsAndConditionsClick,
            onInstallFullPluginClick = onInstallFullPluginClick,
            onContactSupportClick = onContactSupportClick,
        )
}
