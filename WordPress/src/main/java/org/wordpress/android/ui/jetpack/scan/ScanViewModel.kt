package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ScanIdleState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ScanScanningState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import javax.inject.Inject

class ScanViewModel @Inject constructor(
    private val scanStatusService: ScanStatusService
) : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val scanStateObserver = Observer<ScanStateModel> {
        _uiState.value = buildContentUiState(it)
    }

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.site = site
        init()
    }

    private fun init() {
        scanStatusService.scanState.observeForever(scanStateObserver)
        scanStatusService.start(site)
    }

    private fun buildContentUiState(model: ScanStateModel): Content {
        val scanStateItem = when (model.state) {
            ScanStateModel.State.IDLE ->
                model.threats?.let { ScanIdleState.ThreatsFound() }
                    ?: ScanIdleState.ThreatsNotFound()
            ScanStateModel.State.SCANNING -> ScanScanningState()
            ScanStateModel.State.PROVISIONING, ScanStateModel.State.UNAVAILABLE, ScanStateModel.State.UNKNOWN ->
                ScanScanningState() // TODO: ashiagr filter out
        }
        return Content(listOf(scanStateItem))
    }

    override fun onCleared() {
        scanStatusService.scanState.removeObserver(scanStateObserver)
        scanStatusService.stop()
        super.onCleared()
    }

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<ScanListItemState>) : UiState()
    }
}
