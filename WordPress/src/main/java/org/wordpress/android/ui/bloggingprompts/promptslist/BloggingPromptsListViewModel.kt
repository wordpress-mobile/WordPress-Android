package org.wordpress.android.ui.bloggingprompts.promptslist

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class BloggingPromptsListViewModel @Inject constructor(
    private val fetchBloggingPromptsList: FetchBloggingPromptsListUseCase,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _uiStateFlow = MutableStateFlow<UiState>(UiState.None)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    fun fetchPrompts() {
        launch {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                _uiStateFlow.update { UiState.NetworkError }
                return@launch
            }

            _uiStateFlow.update { UiState.Loading }

            try {
                val prompts = fetchBloggingPromptsList.execute()
                _uiStateFlow.update { UiState.Content(prompts) }
            } catch (e: Exception) {
                _uiStateFlow.update { UiState.FetchError }
            }
        }
    }

    sealed interface UiState {
        object None : UiState
        object Loading : UiState
        data class Content(val content: List<BloggingPromptsListItemModel>) : UiState
        object FetchError : UiState
        object NetworkError : UiState
    }
}
