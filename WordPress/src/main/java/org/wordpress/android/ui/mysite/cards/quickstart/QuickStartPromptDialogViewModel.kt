package org.wordpress.android.ui.mysite.cards.quickstart

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

@HiltViewModel
class QuickStartPromptDialogViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper
) : ViewModel() {
    fun onNegativeClicked(isNewSite: Boolean) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            if (isNewSite) {
                appPrefsWrapper.setShouldHideNextStepsDashboardCard(site.siteId, true)
            } else {
                appPrefsWrapper.setShouldHideGetToKnowTheAppDashboardCard(site.siteId,true)
            }
        }
    }
}
