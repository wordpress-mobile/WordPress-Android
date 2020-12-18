package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ScanIdleState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatsHeaderItemState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import javax.inject.Inject

class ScanViewModel @Inject constructor(
    private val scanStatusService: ScanStatusService,
    private val scanStateListItemBuilder: ScanStateListItemBuilder
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
        val items = mutableListOf<ScanListItemState>()

        val scanStateItemState = scanStateListItemBuilder.mapToScanState(
            model,
            site,
            this@ScanViewModel::onScanButtonClicked,
            this@ScanViewModel::onFixAllButtonClicked
        )
        items.add(scanStateItemState)
        model.threats?.takeIf { scanStateItemState is ScanIdleState.ThreatsFound }?.let { threats ->
            items.add(ThreatsHeaderItemState())
            items.addAll(threats.map { ThreatItemState(it) })
        }

        return Content(items)
    }

    fun onScanButtonClicked() { // TODO ashiagr to be implemented
    }

    fun onFixAllButtonClicked() { // TODO ashiagr to be implemented
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
