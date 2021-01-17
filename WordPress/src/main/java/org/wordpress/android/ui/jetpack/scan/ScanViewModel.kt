package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ScanViewModel @Inject constructor(
    private val scanStateListItemsBuilder: ScanStateListItemsBuilder,
    private val fetchScanStateUseCase: FetchScanStateUseCase,
    private val startScanUseCase: StartScanUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<ScanNavigationEvents>>()
    val navigationEvents: LiveData<Event<ScanNavigationEvents>> = _navigationEvents

    private val fixableThreatIds
        get() = (_uiState.value as? Content)?.items?.filterIsInstance(ThreatItemState::class.java)
            ?.filter { it.isFixable }
            ?.map { it.threatId } ?: listOf()

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.site = site
        fetchScanState()
    }

    fun fetchScanState() {
        launch {
            fetchScanStateUseCase.fetchScanState(site = site)
                .collect { state ->
                    when (state) {
                        is FetchScanState.Success -> updateUiState(buildContentUiState(state.scanStateModel))
                        is FetchScanState.Failure -> TODO() // TODO ashiagr to be implemented
                    }
                }
        }
    }

    private fun startScan() {
        launch {
            startScanUseCase.startScan(site)
                .collect { startScanState ->
                    when (startScanState) {
                        is StartScanState.ScanningStateUpdatedInDb -> updateUiState(
                            buildContentUiState(startScanState.model)
                        )

                        is StartScanState.Success -> fetchScanState()

                        is StartScanState.Failure -> TODO() // TODO ashiagr to be implemented
                    }
                }
        }
    }

    private fun fixAllThreats() { // TODO ashiagr to be implemented
    }

    private fun buildContentUiState(model: ScanStateModel) = Content(
        scanStateListItemsBuilder.buildScanStateListItems(
            model,
            site,
            this@ScanViewModel::onScanButtonClicked,
            this@ScanViewModel::onFixAllButtonClicked,
            this@ScanViewModel::onThreatItemClicked
        )
    )

    private fun onScanButtonClicked() {
        startScan()
    }

    private fun onFixAllButtonClicked() {
        updateNavigationEvent(
            OpenFixThreatsConfirmationDialog(
                title = UiStringRes(R.string.threat_fix_all_warning_title),
                message = UiStringResWithParams(
                    R.string.threat_fix_all_warning_message,
                    listOf(UiStringText("${fixableThreatIds.size}"))
                ),
                okButtonAction = this@ScanViewModel::fixAllThreats
            )
        )
    }

    private fun onThreatItemClicked(threatId: Long) {
        _navigationEvents.value = Event(ShowThreatDetails(threatId))
    }

    private fun updateNavigationEvent(navigationEvent: ScanNavigationEvents) {
        _navigationEvents.value = Event(navigationEvent)
    }

    private fun updateUiState(contentState: Content) {
        _uiState.value = contentState
    }

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<JetpackListItemState>) : UiState()
    }
}
