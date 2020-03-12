package org.wordpress.android.ui.sitecreation.verticals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class SiteCreationVerticalsViewModel @Inject constructor() : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

    private var segmentId: Long? = null

    private val _skipBtnClicked = SingleLiveEvent<Unit>()
    val skipBtnClicked: LiveData<Unit> = _skipBtnClicked

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    fun start(segmentId: Long) {
        if (isStarted) {
            return
        }
        this.segmentId = segmentId
        isStarted = true
        updateUiStateToContent()
    }

    fun onSkipStepBtnClicked() {
        _skipBtnClicked.call()
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    private fun updateUiStateToContent() {
        updateUiState(
                VerticalsContentUiState(
                        showSkipButton = true
                )
        )
    }

    private fun updateUiState(uiState: VerticalsUiState) {
        _uiState.value = uiState
    }

    sealed class VerticalsUiState(
        val contentLayoutVisibility: Boolean
    ) {
        data class VerticalsContentUiState(
            val showSkipButton: Boolean
        ) : VerticalsUiState(
                contentLayoutVisibility = true
        )
    }
}
