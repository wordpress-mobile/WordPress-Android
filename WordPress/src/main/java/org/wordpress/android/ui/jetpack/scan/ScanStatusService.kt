package org.wordpress.android.ui.jetpack.scan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.UNAVAILABLE
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.UNKNOWN
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.util.ScanFeatureConfig
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Deprecated(
    "This class is replaced in favour of FetchScanStateUseCase." +
        "It will be removed after scan menu logic built upon it is replaced with scan capabilities logic."
)
@Singleton
class ScanStatusService
@Inject constructor(
    private val scanStore: ScanStore,
    private val scanFeatureConfig: ScanFeatureConfig,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) {
    private var site: SiteModel? = null
    private var fetchScanStateJob: Job? = null

    private val _scanAvailable = MutableLiveData<Boolean>()
    val scanAvailable: LiveData<Boolean> = _scanAvailable

    private val _scanState = MutableLiveData<ScanStateModel>()
    val scanState: LiveData<ScanStateModel>
        get() = _scanState

    private val _scanStateFetchError = MutableLiveData<ScanStateError>()

    fun start(site: SiteModel) {
        if (!scanFeatureConfig.isEnabled()) {
            return
        }
        if (this.site == null) {
            this.site = site
            requestScanStateUpdate()
            reloadScanState()
        }
    }

    fun stop() {
        fetchScanStateJob?.cancel()
        if (site != null) {
            site = null
        }
    }

    private fun requestScanStateUpdate() {
        site?.let {
            fetchScanStateJob?.cancel()
            fetchScanStateJob = uiScope.launch {
                val scanState = scanStore.fetchScanState(FetchScanStatePayload(it))
                onScanStateFetched(scanState.error, scanState.isError)
            }
        }
    }

    private fun reloadScanState(): Boolean {
        site?.let { site ->
            val state = scanStore.getScanStateForSite(site)
            state?.let { scanState ->
                updateScanState(scanState)
                return true
            }
        }
        return false
    }

    private fun updateScanState(scanStateModel: ScanStateModel) {
        _scanAvailable.value = scanStateModel.state != UNAVAILABLE &&
            scanStateModel.state != UNKNOWN &&
            _scanStateFetchError.value == null
        _scanState.value = scanStateModel
    }

    private fun onScanStateFetched(scanStateError: ScanStateError?, isError: Boolean) {
        _scanStateFetchError.value = scanStateError
        reloadScanState()
    }
}
