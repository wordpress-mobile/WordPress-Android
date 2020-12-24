package org.wordpress.android.ui.jetpack.scan.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.usecases.GetThreatDetailsUseCase
import javax.inject.Inject

class ThreatDetailsViewModel @Inject constructor(
    private val getThreatDetailsUseCase: GetThreatDetailsUseCase,
    private val threatDetailsListItemsBuilder: ThreatDetailsListItemsBuilder
) : ViewModel() {
    private var isStarted = false
    private var threatId: Long = 0

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start(threatId: Long) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.threatId = threatId
        getData()
    }

    private fun getData() {
        viewModelScope.launch {
            val threatModel = getThreatDetailsUseCase.get(threatId)
            threatModel?.let { buildContentUiState(threatModel) }
        }
    }

    private fun buildContentUiState(model: ThreatModel) =
        Content(threatDetailsListItemsBuilder.buildThreatDetailsListItems(model))

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<JetpackListItemState>) : UiState()
    }
}
