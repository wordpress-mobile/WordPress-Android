package org.wordpress.android.ui.jpfullplugininstall

import org.wordpress.android.ui.jpfullplugininstall.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.extensions.activeJetpackConnectionPluginNames
import javax.inject.Inject

class JetpackFullPluginInstallOnboardingUiStateMapper @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
) {
    fun mapLoaded(
        onTermsAndConditionsClick: () -> Unit,
        onInstallFullPluginClick: () -> Unit,
        onContactSupportClick: () -> Unit,
    ): UiState.Loaded {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return UiState.Loaded(
            siteName = selectedSite?.name.orEmpty(),
            pluginNames = selectedSite?.activeJetpackConnectionPluginNames().orEmpty(),
            onTermsAndConditionsClick = onTermsAndConditionsClick,
            onInstallFullPluginClick = onInstallFullPluginClick,
            onContactSupportClick = onContactSupportClick,
        )
    }
}
