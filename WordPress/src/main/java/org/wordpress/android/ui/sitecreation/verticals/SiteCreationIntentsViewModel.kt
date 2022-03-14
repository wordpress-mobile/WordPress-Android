package org.wordpress.android.ui.sitecreation.verticals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class SiteCreationIntentsViewModel @Inject constructor(
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job

    private var isStarted = false

    private val _onSkipButtonPressed = SingleLiveEvent<Unit>()
    val onSkipButtonPressed: LiveData<Unit> = _onSkipButtonPressed

    private val _onBackButtonPressed = SingleLiveEvent<Unit>()
    val onBackButtonPressed: LiveData<Unit> = _onBackButtonPressed

    private val _uiState = MutableLiveData<IntentsUiState>()
    val uiState: LiveData<IntentsUiState> = _uiState

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        // tracker.trackSiteIntentQuestionViewed()
        resetUiState()
    }

    private fun resetUiState() {
        updateUiState(
                IntentsUiState()
        )
    }

    private fun updateUiState(uiState: IntentsUiState) {
        _uiState.value = uiState
    }

    fun onSkipPressed() {
        // tracker.trackSiteIntentQuestionSkipped()
        _onSkipButtonPressed.call()
    }

    fun onBackPressed() {
        // tracker.trackSiteIntentQuestionCanceled()
        _onBackButtonPressed.call()
    }

    fun onAppBarOffsetChanged(verticalOffset: Int, scrollThreshold: Int) {
        val headerShouldBeVisible = verticalOffset < scrollThreshold
        uiState.value?.let { state ->
            if (state.isHeaderVisible == headerShouldBeVisible) return // No change
            updateUiState(state.copy(isHeaderVisible = headerShouldBeVisible))
        }
    }

    data class IntentsUiState(
        val isHeaderVisible: Boolean = false
    )
}
