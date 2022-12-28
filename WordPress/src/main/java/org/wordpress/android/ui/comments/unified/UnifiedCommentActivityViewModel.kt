package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class UnifiedCommentActivityViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState: MutableStateFlow<CommentListActivityUiModel> = MutableStateFlow(
        CommentListActivityUiModel(
            true
        )
    )
    val uiState: StateFlow<CommentListActivityUiModel> = _uiState

    fun onActionModeToggled(isEnabled: Boolean) {
        launch {
            _uiState.emit(
                CommentListActivityUiModel(
                    !isEnabled
                )
            )
        }
    }

    data class CommentListActivityUiModel(val isTabBarEnabled: Boolean)
}
