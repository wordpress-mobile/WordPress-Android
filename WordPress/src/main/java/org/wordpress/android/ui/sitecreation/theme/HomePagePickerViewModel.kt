package org.wordpress.android.ui.sitecreation.theme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class HomePagePickerViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        updateUiState(UiState.Content())
    }

    /**
     * Appbar scrolled event
     * @param verticalOffset the scroll state vertical offset
     * @param scrollThreshold the scroll threshold
     */
    fun onAppBarOffsetChanged(verticalOffset: Int, scrollThreshold: Int) {
        setHeaderTitleVisibility(verticalOffset < scrollThreshold)
    }

    fun onPreviewTapped() {
        // TODO
    }

    fun onChooseTapped() {
        // TODO
    }

    fun onSkippedTapped() {
        // FIXME: This is temporary for PR #13192 to showcase the toolbar show/hide mechanism
        (uiState.value as? UiState.Content)?.let { state ->
            updateUiState(state.copy(isToolbarVisible = !state.isToolbarVisible))
        }
    }

    fun onBackPressed() {
        // TODO
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    private fun setHeaderTitleVisibility(headerShouldBeVisible: Boolean) {
        (uiState.value as? UiState.Content)?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    sealed class UiState(
        open val isHeaderVisible: Boolean = false,
        open val isToolbarVisible: Boolean = false
    ) {
        object Loading : UiState()

        data class Content(
            override val isHeaderVisible: Boolean = false,
            override val isToolbarVisible: Boolean = false
        ) : UiState()

        class Error : UiState()
    }
}
