package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

const val FETCH_SCAN_STATE_DELAY_MILLIS = 5000L

class FetchScanStateUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun fetchScanState(
        site: SiteModel,
        polling: Boolean = true,
        delayInMs: Long = FETCH_SCAN_STATE_DELAY_MILLIS
    ): Flow<FetchScanState> = flow {
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(Failure.NetworkUnavailable)
                return@flow
            }

            val result = scanStore.fetchScanState(FetchScanStatePayload(site))
            if (result.isError) {
                emit(Failure.RemoteRequestFailure)
                return@flow
            } else {
                val scanStateModel = scanStore.getScanStateForSite(site)
                if (scanStateModel != null) {
                    emit(Success(scanStateModel))
                    if (polling && scanStateModel.state == ScanStateModel.State.SCANNING) {
                        delay(delayInMs)
                    } else {
                        return@flow
                    }
                } else {
                    emit(Failure.RemoteRequestFailure)
                    return@flow
                }
            }
        }
    }.flowOn(bgDispatcher)

    sealed class FetchScanState {
        data class Success(val scanStateModel: ScanStateModel) : FetchScanState()
        sealed class Failure : FetchScanState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
        }
    }
}
