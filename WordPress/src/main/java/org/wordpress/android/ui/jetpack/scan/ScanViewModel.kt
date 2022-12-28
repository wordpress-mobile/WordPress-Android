package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowContactSupport
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.VisitVaultPressDashboard
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.FullScreenLoadingUiState
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.util.analytics.ScanTracker.ErrorAction
import org.wordpress.android.util.analytics.ScanTracker.ErrorCause
import org.wordpress.android.util.analytics.ScanTracker.OnThreatItemClickSource
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val RETRY_DELAY = 300L

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanStateListItemsBuilder: ScanStateListItemsBuilder,
    private val fetchScanStateUseCase: FetchScanStateUseCase,
    private val startScanUseCase: StartScanUseCase,
    private val fixThreatsUseCase: FixThreatsUseCase,
    private val fetchFixThreatsStatusUseCase: FetchFixThreatsStatusUseCase,
    private val scanStore: ScanStore,
    private val scanTracker: ScanTracker,
    private val htmlMessageUtils: HtmlMessageUtils,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _navigationEvents = MediatorLiveData<Event<ScanNavigationEvents>>()
    val navigationEvents: LiveData<Event<ScanNavigationEvents>> = _navigationEvents

    private val fixableThreatIds
        get() = scanStateModel?.threats
            ?.filter { it.baseThreatModel.fixable != null }
            ?.map { it.baseThreatModel.id }
            ?: emptyList()

    private var scanStateModel: ScanStateModel? = null
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
        updateUiState(FullScreenLoadingUiState)
        launch {
            scanStateModel = scanStore.getScanStateForSite(this@ScanViewModel.site)
            scanStateModel?.let {
                if (fixableThreatIds.isNotEmpty()) fetchFixThreatsStatus(fixableThreatIds, isInvokedByUser = false)
            }
            fetchScanState(isInvokedFromInit = true)
        }
    }

    private suspend fun fetchScanState(
        invokedByUser: Boolean = false,
        isRetry: Boolean = false,
        isInvokedFromInit: Boolean = false
    ) {
        if (isRetry) delay(RETRY_DELAY)

        fetchScanStateUseCase.fetchScanState(site = site, startWithDelay = invokedByUser)
            .collect { state ->
                when (state) {
                    is FetchScanState.Success -> {
                        scanStateModel = state.scanStateModel
                        updateUiState(buildContentUiState(state.scanStateModel))
                        if (state.scanStateModel.state in listOf(State.UNAVAILABLE, State.UNKNOWN)) {
                            scanTracker.trackOnError(ErrorAction.FETCH_SCAN_STATE, ErrorCause.OTHER)
                            updateUiState(ErrorUiState.ScanRequestFailed(::onContactSupportClicked))
                        } else if (invokedByUser && state.scanStateModel.state == State.IDLE) {
                            showScanFinishedMessage(state.scanStateModel)
                        }
                    }

                    is FetchScanState.Failure.NetworkUnavailable -> {
                        scanTracker.trackOnError(ErrorAction.FETCH_SCAN_STATE, ErrorCause.OFFLINE)
                        scanStateModel?.takeIf { !isInvokedFromInit }
                            ?.let {
                                updateSnackbarMessageEvent(UiStringRes(R.string.error_generic_network))
                            }
                            ?: updateUiState(ErrorUiState.NoConnection(::onRetryClicked))
                    }

                    is FetchScanState.Failure.MultisiteNotSupported ->
                        updateUiState(ErrorUiState.MultisiteNotSupported)

                    is FetchScanState.Failure.VaultPressActiveOnSite ->
                        updateUiState(ErrorUiState.VaultPressActiveOnSite(::onVisitVaultPressDashboardClicked))

                    is FetchScanState.Failure.RemoteRequestFailure -> {
                        scanTracker.trackOnError(ErrorAction.FETCH_SCAN_STATE, ErrorCause.REMOTE)
                        scanStateModel?.takeIf { !isInvokedFromInit }
                            ?.let {
                                updateSnackbarMessageEvent(UiStringRes(R.string.request_failed_message))
                            }
                            ?: updateUiState(ErrorUiState.GenericRequestFailed(::onContactSupportClicked))
                    }
                }
            }
    }

    private fun showScanFinishedMessage(scanStateModel: ScanStateModel) {
        val threatsCount = scanStateModel.threats?.size ?: 0
        val message = UiStringText(
            when (threatsCount) {
                0 -> htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                    R.string.scan_finished_no_threats_found_message
                )

                1 -> htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                    R.string.scan_finished_potential_threats_found_message_singular
                )

                else -> htmlMessageUtils.getHtmlMessageFromStringFormatResId(
                    R.string.scan_finished_potential_threats_found_message_plural,
                    "$threatsCount"
                )
            }
        )
        updateSnackbarMessageEvent(message)
    }

    private fun startScan() {
        launch {
            startScanUseCase.startScan(site)
                .collect { state ->
                    when (state) {
                        is StartScanState.ScanningStateUpdatedInDb ->
                            updateUiState(buildContentUiState(state.model))

                        is StartScanState.Success -> fetchScanState(invokedByUser = true)

                        is StartScanState.Failure.NetworkUnavailable -> {
                            scanTracker.trackOnError(ErrorAction.SCAN, ErrorCause.OFFLINE)
                            updateSnackbarMessageEvent(UiStringRes(R.string.error_generic_network))
                        }
                        is StartScanState.Failure.RemoteRequestFailure -> {
                            scanTracker.trackOnError(ErrorAction.SCAN, ErrorCause.REMOTE)
                            updateUiState(ContentUiState(emptyList()))
                            updateUiState(ErrorUiState.ScanRequestFailed(::onContactSupportClicked))
                        }
                    }
                }
        }
    }

    private fun fixAllThreats() {
        scanTracker.trackOnFixAllThreatsConfirmed()
        launch {
            when (fixThreatsUseCase.fixThreats(remoteSiteId = site.siteId, fixableThreatIds = fixableThreatIds)) {
                is FixThreatsState.Success -> {
                    val someOrAllThreatFixed = fetchFixThreatsStatus(fixableThreatIds, isInvokedByUser = true)
                    if (someOrAllThreatFixed) fetchScanState()
                }
                is FixThreatsState.Failure.NetworkUnavailable -> {
                    scanTracker.trackOnError(ErrorAction.FIX_ALL, ErrorCause.OFFLINE)
                    updateSnackbarMessageEvent(UiStringRes(R.string.error_generic_network))
                }
                is FixThreatsState.Failure.RemoteRequestFailure -> {
                    scanTracker.trackOnError(ErrorAction.FIX_ALL, ErrorCause.REMOTE)
                    updateSnackbarMessageEvent(UiStringRes(R.string.threat_fix_all_error_message))
                }
            }
        }
    }

    private suspend fun fetchFixThreatsStatus(fixableThreatIds: List<Long>, isInvokedByUser: Boolean): Boolean {
        var someOrAllThreatFixed = false

        @StringRes var messageRes: Int? = null
        fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(
            remoteSiteId = site.siteId,
            fixableThreatIds = fixableThreatIds
        ).collect { status ->
            var fixingThreatIds = emptyList<Long>()
            when (status) {
                is FetchFixThreatsState.NotStarted -> { // Do nothing
                }
                is FetchFixThreatsState.InProgress -> {
                    fixingThreatIds = status.threatIds
                }
                is FetchFixThreatsState.Complete -> {
                    someOrAllThreatFixed = true
                    messageRes = if (status.fixedThreatsCount == 1) {
                        R.string.threat_fix_all_status_success_message_singular
                    } else R.string.threat_fix_all_status_success_message_plural
                }
                is FetchFixThreatsState.Failure.NetworkUnavailable -> {
                    scanTracker.trackOnError(ErrorAction.FETCH_FIX_THREAT_STATUS, ErrorCause.OFFLINE)
                    messageRes = R.string.error_generic_network
                }
                is FetchFixThreatsState.Failure.RemoteRequestFailure -> {
                    scanTracker.trackOnError(ErrorAction.FETCH_FIX_THREAT_STATUS, ErrorCause.REMOTE)
                    messageRes = R.string.threat_fix_all_status_error_message
                }
                is FetchFixThreatsState.Failure.FixFailure -> {
                    if (status.mightBeMissingCredentials) {
                        scanTracker.trackOnError(ErrorAction.FETCH_FIX_THREAT_STATUS, ErrorCause.ALL_THREATS_NOT_FIXED)
                    } else {
                        scanTracker.trackOnError(ErrorAction.FETCH_FIX_THREAT_STATUS, ErrorCause.OTHER)
                    }
                    if (!status.containsOnlyErrors) {
                        someOrAllThreatFixed = true
                    } else {
                        messageRes = R.string.threat_fix_all_status_error_message
                    }
                }
            }

            val shouldUpdateUi = status is FetchFixThreatsState.InProgress || status is FetchFixThreatsState.Complete
            if (shouldUpdateUi) {
                updateUiState(
                    buildContentUiState(model = requireNotNull(scanStateModel), fixingThreatIds = fixingThreatIds)
                )
            }

            if (isInvokedByUser) messageRes?.let { updateSnackbarMessageEvent(UiStringRes(it)) }
        }

        return someOrAllThreatFixed
    }

    private fun onRetryClicked() {
        launch { fetchScanState(isRetry = true) }
    }

    private fun onVisitVaultPressDashboardClicked() {
        updateNavigationEvent(VisitVaultPressDashboard(Constants.URL_VISIT_VAULTPRESS_DASHBOARD))
    }

    private fun onContactSupportClicked() {
        updateNavigationEvent(ShowContactSupport(site))
    }

    private fun onScanButtonClicked() {
        scanTracker.trackOnScanButtonClicked()
        startScan()
    }

    private fun onFixAllButtonClicked() {
        scanTracker.trackOnFixAllThreatsButtonClicked()
        updateNavigationEvent(
            OpenFixThreatsConfirmationDialog(
                title = UiStringRes(R.string.threat_fix_all_warning_title),
                message = if (fixableThreatIds.size > 1) {
                    UiStringResWithParams(
                        R.string.threat_fix_all_confirmation_message_plural,
                        listOf(UiStringText("${fixableThreatIds.size}"))
                    )
                } else UiStringRes(R.string.threat_fix_all_confirmation_message_singular),
                okButtonAction = this@ScanViewModel::fixAllThreats
            )
        )
    }

    private fun onThreatItemClicked(threatId: Long) {
        launch {
            scanTracker.trackOnThreatItemClicked(threatId, OnThreatItemClickSource.SCANNER)
            _navigationEvents.value = Event(ShowThreatDetails(site, threatId))
        }
    }

    private fun onEnterServerCredsIconClicked() {
        updateNavigationEvent(ShowJetpackSettings("${Constants.URL_JETPACK_SETTINGS}/${site.siteId}"))
    }

    fun onScanStateRequestedWithMessage(@StringRes messageRes: Int) {
        updateSnackbarMessageEvent(UiStringRes(messageRes))
        launch { fetchScanState() }
    }

    fun onFixStateRequested(threatId: Long) {
        launch {
            val isThreatFixed = fetchFixThreatsStatus(listOf(threatId), isInvokedByUser = true)
            if (isThreatFixed) fetchScanState()
        }
    }

    private fun updateSnackbarMessageEvent(message: UiString) {
        _snackbarEvents.value = Event(SnackbarMessageHolder(message))
    }

    private fun updateNavigationEvent(navigationEvent: ScanNavigationEvents) {
        _navigationEvents.value = Event(navigationEvent)
    }

    private fun updateUiState(state: UiState) {
        _uiState.value = state
    }

    private suspend fun buildContentUiState(
        model: ScanStateModel,
        fixingThreatIds: List<Long> = emptyList()
    ) = ContentUiState(
        scanStateListItemsBuilder.buildScanStateListItems(
            model = model,
            site = site,
            fixingThreatIds = fixingThreatIds,
            onScanButtonClicked = this@ScanViewModel::onScanButtonClicked,
            onFixAllButtonClicked = this@ScanViewModel::onFixAllButtonClicked,
            onThreatItemClicked = this@ScanViewModel::onThreatItemClicked,
            onHelpClicked = this@ScanViewModel::onContactSupportClicked,
            onEnterServerCredsIconClicked = this@ScanViewModel::onEnterServerCredsIconClicked
        )
    )

    sealed class UiState(
        val loadingVisible: Boolean = false,
        val contentVisible: Boolean = false,
        val errorVisible: Boolean = false
    ) {
        object FullScreenLoadingUiState : UiState(loadingVisible = true)

        data class ContentUiState(val items: List<JetpackListItemState>) : UiState(contentVisible = true)

        sealed class ErrorUiState : UiState(errorVisible = true) {
            abstract val image: Int
            open val imageColorResId: Int? = null
            abstract val title: UiString
            abstract val subtitle: UiString
            open val buttonText: UiString? = null
            open val action: (() -> Unit)? = null

            data class NoConnection(override val action: () -> Unit) : ErrorUiState() {
                @DrawableRes
                override val image = R.drawable.img_illustration_cloud_off_152dp
                override val title = UiStringRes(R.string.scan_no_network_title)
                override val subtitle = UiStringRes(R.string.scan_no_network_subtitle)
                override val buttonText = UiStringRes(R.string.retry)
            }

            data class GenericRequestFailed(override val action: () -> Unit) : ErrorUiState() {
                @DrawableRes
                override val image = R.drawable.img_illustration_cloud_off_152dp
                override val title = UiStringRes(R.string.scan_request_failed_title)
                override val subtitle = UiStringRes(R.string.scan_request_failed_subtitle)
                override val buttonText = UiStringRes(R.string.contact_support)
            }

            data class ScanRequestFailed(override val action: () -> Unit) : ErrorUiState() {
                @DrawableRes
                override val image = R.drawable.img_illustration_empty_results_216dp
                override val title = UiStringRes(R.string.scan_start_request_failed_title)
                override val subtitle = UiStringRes(R.string.scan_start_request_failed_subtitle)
                override val buttonText = UiStringRes(R.string.contact_support)
            }

            object MultisiteNotSupported : ErrorUiState() {
                @DrawableRes
                override val image = R.drawable.ic_baseline_security_white_24dp
                @ColorRes
                override val imageColorResId = R.color.gray
                override val title = UiStringRes(R.string.scan_multisite_not_supported_title)
                override val subtitle = UiStringRes(R.string.scan_multisite_not_supported_subtitle)
            }

            data class VaultPressActiveOnSite(override val action: () -> Unit) : ErrorUiState() {
                @DrawableRes
                override val image = R.drawable.ic_shield_warning_white
                @ColorRes
                override val imageColorResId = R.color.error_60
                override val title = UiStringRes(R.string.scan_vault_press_active_on_site_title)
                override val subtitle = UiStringRes(R.string.scan_vault_press_active_on_site_subtitle)
                override val buttonText = UiStringRes(R.string.scan_vault_press_active_on_site_button_text)
            }
        }
    }
}
