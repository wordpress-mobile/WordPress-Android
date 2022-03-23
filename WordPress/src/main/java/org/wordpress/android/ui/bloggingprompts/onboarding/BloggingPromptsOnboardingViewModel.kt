package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.models.usecases.GetBloggingPromptUseCase
import org.wordpress.android.ui.bloggingprompts.onboarding.Action.OpenEditor
import javax.inject.Inject

class BloggingPromptsOnboardingViewModel @Inject constructor(
    private val getBloggingPromptUseCase: GetBloggingPromptUseCase
) : ViewModel() {
    private lateinit var bloggingPrompt: BloggingPrompt

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _action = MutableLiveData<Action>()
    val action: LiveData<Action> = _action

    fun start() {
        viewModelScope.launch {
            bloggingPrompt = getBloggingPromptUseCase.execute().single()
        }
    }

    fun onTryNow() {
        _action.value = OpenEditor(bloggingPrompt)
    }
}
