package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class BloggingPromptsOnboardingViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : ViewModel() {
    private val _uiState = MutableLiveData<BloggingPromptsOnboardingUiState>()
    val uiState: LiveData<BloggingPromptsOnboardingUiState> = _uiState

    private val _action = MutableLiveData<BloggingPromptsOnboardingAction>()
    val action: LiveData<BloggingPromptsOnboardingAction> = _action

    fun start() {
        _uiState.value = uiStateMapper.mapReady()
    }

    fun onTryNowClick() {
        // TODO send BloggingPrompt with OpenEditor action when prompt store is ready
        _action.value = OpenEditor
    }

    fun onRemindMeClick() {
        if (siteStore.sitesCount > 1) {
            _action.value = OpenSitePicker(selectedSiteRepository.getSelectedSite())
        } else {
            siteStore.sites.firstOrNull()?.let {
                _action.value = OpenRemindersIntro(it.id)
            }
        }
    }

    fun onSiteSelected(selectedSiteLocalId: Int) {
        _action.value = OpenRemindersIntro(selectedSiteLocalId)
    }
}
