package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.action.ScanAction.START_SCAN
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.Reason
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.SCANNING
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.ScanStartPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.ScanningStateUpdatedInDb
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class StartScanUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
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
    }.flowOn(bgDispatcher)

    private suspend fun FlowCollector<StartScanState>.updateScanScanningStateInDb(site: SiteModel) {
        val model = scanStore.getScanStateForSite(site)?.copy(state = SCANNING) ?: ScanStateModel(
            state = SCANNING,
            reason = Reason.NO_REASON
        )
        scanStore.addOrUpdateScanStateModelForSite(START_SCAN, site, model)
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
