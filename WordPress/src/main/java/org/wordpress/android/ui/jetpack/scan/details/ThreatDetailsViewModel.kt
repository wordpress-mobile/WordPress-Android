package org.wordpress.android.ui.jetpack.scan.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.usecases.GetThreatModelUseCase
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import javax.inject.Inject

class ThreatDetailsViewModel @Inject constructor(
    private val getThreatModelUseCase: GetThreatModelUseCase,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val builder: ThreatDetailsListItemsBuilder
) : ViewModel() {
    private lateinit var site: SiteModel
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
        site = requireNotNull(selectedSiteRepository.getSelectedSite())
        getData()
    }

    private fun getData() {
        viewModelScope.launch {
            val model = getThreatModelUseCase.get(threatId)
            model?.let { updateUiState(it) }
        }
    }

    private fun updateUiState(model: ThreatModel) {
        _uiState.value = buildContentUiState(model)
    }

    private fun buildContentUiState(model: ThreatModel) = Content(
        builder.buildThreatDetailsListItems(
            model,
            this@ThreatDetailsViewModel::onFixThreatButtonClicked,
            this@ThreatDetailsViewModel::onGetFreeEstimateButtonClicked,
            this@ThreatDetailsViewModel::onIgnoreThreatButtonClicked
        )
    )

    private fun onFixThreatButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun onIgnoreThreatButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun onGetFreeEstimateButtonClicked() { // TODO ashiagr to be implemented
    }

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<JetpackListItemState>) : UiState()
    }
}
