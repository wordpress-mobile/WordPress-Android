package org.wordpress.android.ui.jetpack.scan.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.OpenIgnoreThreatActionDialog
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.ShowUpdatedScanState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.usecases.GetThreatModelUseCase
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase.IgnoreThreatState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

private const val DELAY_MILLIS = 2000L

class ThreatDetailsViewModel @Inject constructor(
    private val getThreatModelUseCase: GetThreatModelUseCase,
    private val ignoreThreatUseCase: IgnoreThreatUseCase,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val builder: ThreatDetailsListItemsBuilder,
    private val htmlMessageUtils: HtmlMessageUtils,
    private val resourceProvider: ResourceProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel() {
    private lateinit var site: SiteModel
    private var isStarted = false
    private var threatId: Long = 0

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _navigationEvents = MediatorLiveData<Event<ThreatDetailsNavigationEvents>>()
    val navigationEvents: LiveData<Event<ThreatDetailsNavigationEvents>> = _navigationEvents

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
            model?.let { updateUiState(buildContentUiState(it)) }
        }
    }

    private fun ignoreThreat() {
        viewModelScope.launch {
            updateThreatActionButtons(isEnabled = false)
            when (ignoreThreatUseCase.ignoreThreat(site.siteId, threatId)) {
                is IgnoreThreatState.Success -> {
                    // TODO ashiagr consider showing success message in the scan state screen
                    updateSnackbarMessageEvent(UiStringRes(R.string.threat_ignore_success_message))

                    withContext(bgDispatcher) { delay(DELAY_MILLIS) }
                    updateNavigationEvent(ShowUpdatedScanState)
                }

                is IgnoreThreatState.Failure.NetworkUnavailable -> {
                    updateThreatActionButtons(isEnabled = true)
                    updateSnackbarMessageEvent(UiStringRes(R.string.error_generic_network))
                }

                is IgnoreThreatState.Failure.RemoteRequestFailure -> {
                    updateThreatActionButtons(isEnabled = true)
                    updateSnackbarMessageEvent(UiStringRes(R.string.threat_ignore_error_message))
                }
            }
        }
    }

    private fun onFixThreatButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun onIgnoreThreatButtonClicked() {
        updateNavigationEvent(
            OpenIgnoreThreatActionDialog(
                title = UiStringRes(R.string.threat_ignore),
                message = UiStringText(
                    htmlMessageUtils
                        .getHtmlMessageFromStringFormatResId(
                            R.string.threat_ignore_warning,
                            "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
                        )
                ),
                okButtonAction = this@ThreatDetailsViewModel::ignoreThreat
            )
        )
    }

    private fun onGetFreeEstimateButtonClicked() { // TODO ashiagr to be implemented
    }

    private fun updateThreatActionButtons(isEnabled: Boolean) {
        (_uiState.value as? Content)?.let { content ->
            val updatesContentItems = content.items.map { contentItem ->
                if (contentItem is ActionButtonState) {
                    contentItem.copy(isEnabled = isEnabled)
                } else {
                    contentItem
                }
            }
            updateUiState(content.copy(items = updatesContentItems))
        }
    }

    private fun updateSnackbarMessageEvent(message: UiString) {
        _snackbarEvents.value = Event(SnackbarMessageHolder((message)))
    }

    private fun updateNavigationEvent(navigationEvent: ThreatDetailsNavigationEvents) {
        _navigationEvents.value = Event(navigationEvent)
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private fun buildContentUiState(model: ThreatModel) = Content(
        builder.buildThreatDetailsListItems(
            model,
            this@ThreatDetailsViewModel::onFixThreatButtonClicked,
            this@ThreatDetailsViewModel::onGetFreeEstimateButtonClicked,
            this@ThreatDetailsViewModel::onIgnoreThreatButtonClicked
        )
    )

    sealed class UiState { // TODO: ashiagr add states for loading, error as needed
        data class Content(val items: List<JetpackListItemState>) : UiState()
    }
}
