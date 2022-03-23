package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.models.usecases.GetBloggingPromptUseCase
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import javax.inject.Inject

class BloggingPromptsOnboardingViewModel @Inject constructor(
    private val getBloggingPromptUseCase: GetBloggingPromptUseCase
) : ViewModel() {
    private lateinit var bloggingPrompt: BloggingPrompt

    private val _uiState = MutableLiveData<BloggingPromptsOnboardingUiState>()
    val uiState: LiveData<BloggingPromptsOnboardingUiState> = _uiState

    private val _action = MutableLiveData<BloggingPromptsOnboardingAction>()
    val action: LiveData<BloggingPromptsOnboardingAction> = _action

    fun start() {
        viewModelScope.launch {
            bloggingPrompt = getBloggingPromptUseCase.execute().single()
        }
    }

    fun onTryNow() {
        _action.value = OpenEditor(bloggingPrompt)
    }
}
