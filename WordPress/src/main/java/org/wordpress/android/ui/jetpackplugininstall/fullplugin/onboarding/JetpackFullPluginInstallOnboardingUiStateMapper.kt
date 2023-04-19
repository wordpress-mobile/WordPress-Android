package org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding

import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.JetpackFullPluginInstallOnboardingViewModel.UiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.extensions.activeIndividualJetpackPluginNames
import javax.inject.Inject

class JetpackFullPluginInstallOnboardingUiStateMapper @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
) {
    fun mapLoaded(): UiState.Loaded {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        return UiState.Loaded(
            siteUrl = selectedSite?.let { SiteUtils.getHomeURLOrHostName(it) }.orEmpty(),
            pluginNames = selectedSite?.activeIndividualJetpackPluginNames().orEmpty(),
        )
    }
}
