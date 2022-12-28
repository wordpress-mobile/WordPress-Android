package org.wordpress.android.ui.jetpack.scan.history

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.FIXED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.IGNORED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState.NoConnection
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState.RequestFailed
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Success
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

private const val RETRY_DELAY = 300L

@HiltViewModel
class ScanHistoryViewModel @Inject constructor(
    private val scanTracker: ScanTracker,
    private val fetchScanHistoryUseCase: FetchScanHistoryUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _threats = MutableLiveData<List<ThreatModel>>()
    val threats: LiveData<List<ThreatModel>> = _threats

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    lateinit var site: SiteModel

    fun start(site: SiteModel) {
        if (isStarted) return
        isStarted = true
        this.site = site
        fetchScanHistory()
    }

    private fun fetchScanHistory(isRetry: Boolean = false) {
        launch {
            _uiState.value = ContentUiState
            if (isRetry) {
                delay(RETRY_DELAY)
            }
            when (val result = fetchScanHistoryUseCase.fetch(site)) {
                is Success -> _threats.postValue(result.threatModels)
                is NetworkUnavailable -> _uiState.value = NoConnection(this@ScanHistoryViewModel::onRetryClicked)
                is RemoteRequestFailure -> _uiState.value = RequestFailed(this@ScanHistoryViewModel::onRetryClicked)
            }
        }
    }

    private fun onRetryClicked() {
        fetchScanHistory(true)
    }

    fun onTabSelected(position: Int) {
        scanTracker.trackOnScanHistoryTabSelected(ContentUiState.tabs[position].type)
    }

    data class TabUiState(val label: UiString, val type: ScanHistoryTabType)

    sealed class UiState(val contentVisible: Boolean = false, val errorVisible: Boolean = false) {
        object ContentUiState : UiState(contentVisible = true) {
            val tabs = listOf(
                TabUiState(UiStringRes(R.string.scan_history_all_threats_tab), ALL),
                TabUiState(UiStringRes(R.string.scan_history_fixed_threats_tab), FIXED),
                TabUiState(UiStringRes(R.string.scan_history_ignored_threats_tab), IGNORED)
            )
        }

        sealed class ErrorUiState : UiState(errorVisible = true) {
            abstract val title: UiString
            abstract val img: Int
            abstract val retry: () -> Unit

            data class NoConnection(override val retry: () -> Unit) : ErrorUiState() {
                override val title: UiString = UiStringRes(R.string.scan_history_no_connection)
                @DrawableRes
                override val img: Int = R.drawable.img_illustration_cloud_off_152dp
            }

            data class RequestFailed(override val retry: () -> Unit) : ErrorUiState() {
                override val title: UiString = UiStringRes(R.string.scan_history_request_failed)
                @DrawableRes
                override val img: Int = R.drawable.img_illustration_cloud_off_152dp
            }
        }
    }

    @Parcelize
    @SuppressLint("ParcelCreator")
    enum class ScanHistoryTabType : Parcelable {
        ALL, FIXED, IGNORED
    }
}
