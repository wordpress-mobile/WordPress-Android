package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.SCANNING
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchedScanStatePayload
import org.wordpress.android.fluxc.store.ScanStore.ScanStartPayload
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.ScanningStateUpdatedInDb
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class StartScanUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore
) {
    suspend fun startScan(site: SiteModel) = flow {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            emit(Failure.NetworkUnavailable)
            return@flow
        } else {
            /** Update scanning state in Db to start scan optimistically */
            updateScanScanningStateInDb(site)

            val result = scanStore.startScan(ScanStartPayload(site))
            if (result.isError) {
                emit(Failure.RemoteRequestFailure)
                return@flow
            } else {
                emit(Success)
                return@flow
            }
        }
    }

    private suspend fun FlowCollector<StartScanState>.updateScanScanningStateInDb(site: SiteModel) {
        val model = scanStore.getScanStateForSite(site)?.copy(state = SCANNING) ?: ScanStateModel(state = SCANNING)
        scanStore.storeScanState(FetchedScanStatePayload(model, site))
        emit(ScanningStateUpdatedInDb(model))
    }

    sealed class StartScanState {
        data class ScanningStateUpdatedInDb(val model: ScanStateModel) : StartScanState()
        object Success : StartScanState()
        sealed class Failure : StartScanState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
        }
    }
}
