package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import javax.inject.Inject

class BloggingPromptsOnboardingViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper
) : ViewModel() {
    private lateinit var bloggingPrompt: BloggingPrompt

    private val _uiState = MutableLiveData<BloggingPromptsOnboardingUiState>()
    val uiState: LiveData<BloggingPromptsOnboardingUiState> = _uiState

    private val _action = MutableLiveData<BloggingPromptsOnboardingAction>()
    val action: LiveData<BloggingPromptsOnboardingAction> = _action

    fun start() {
        _uiState.value = uiStateMapper.mapReady()
    }

    fun onTryNowClick() {
        _action.value = OpenEditor(bloggingPrompt)
    }

    fun onRemindMeClick() {
        if (siteStore.sitesCount > 1) {
            _action.value = OpenSitePicker
        } else {
            _action.value = OpenRemindersIntro
        }
    }
}
