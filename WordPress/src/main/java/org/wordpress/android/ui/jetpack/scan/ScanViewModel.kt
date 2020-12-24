package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ScanViewModel @Inject constructor(
    private val scanStatusService: ScanStatusService,
    private val scanStateListItemBuilder: ScanStateListItemBuilder
) : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<ScanNavigationEvents>>()
    val navigationEvents: LiveData<Event<ScanNavigationEvents>> = _navigationEvents

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

    private fun buildContentUiState(model: ScanStateModel) = Content(
        scanStateListItemBuilder.buildScanStateListItems(
            model,
            site,
            this@ScanViewModel::onScanButtonClicked,
            this@ScanViewModel::onFixAllButtonClicked,
            this@ScanViewModel::onThreatItemClicked
        )
    )

    private fun onScanButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun onFixAllButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun onThreatItemClicked(threatId: Long) {
        _navigationEvents.value = Event(ShowThreatDetails(threatId))
    }

    override fun onCleared() {
        scanStatusService.scanState.removeObserver(scanStateObserver)
        scanStatusService.stop()
        super.onCleared()
    }

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<JetpackListItemState>) : UiState()
    }
}
