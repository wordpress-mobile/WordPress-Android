package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanStatePayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.restore.usecases.DELAY_FACTOR
import org.wordpress.android.ui.jetpack.restore.usecases.DELAY_MILLIS
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

const val START_WITH_DELAY_MILLIS = 5000L
const val FETCH_SCAN_STATE_DELAY_MILLIS = 1000L
const val MAX_RETRY = 3

class FetchScanStateUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val tag = javaClass.simpleName

    @Suppress("ComplexMethod", "LoopWithTooManyJumpStatements")
    suspend fun fetchScanState(
        site: SiteModel,
        startWithDelay: Boolean = false
    ): Flow<FetchScanState> = flow {
        var retryAttempts = 0
        if (startWithDelay) {
            delay(START_WITH_DELAY_MILLIS)
        }

        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                val retryAttemptsExceeded = handleError(retryAttempts++, Failure.NetworkUnavailable)
                if (retryAttemptsExceeded) break else continue
            }

            val result = scanStore.fetchScanState(FetchScanStatePayload(site))
            if (result.isError) {
                val retryAttemptsExceeded = handleError(retryAttempts++, Failure.RemoteRequestFailure)
                if (retryAttemptsExceeded) break else continue
            }
            val scanStateModel = scanStore.getScanStateForSite(site)
            if (scanStateModel == null) {
                val retryAttemptsExceeded = handleError(retryAttempts++, Failure.RemoteRequestFailure)
                if (retryAttemptsExceeded) break else continue
            }
            if (scanStateModel.reason == ScanStateModel.Reason.MULTISITE_NOT_SUPPORTED) {
                emit(Failure.MultisiteNotSupported)
                return@flow
            }
            if (scanStateModel.reason == ScanStateModel.Reason.VP_ACTIVE_ON_SITE) {
                emit(Failure.VaultPressActiveOnSite)
                return@flow
            }
            emit(Success(scanStateModel))

            if (scanStateModel.state != ScanStateModel.State.SCANNING) {
                return@flow
            }
            retryAttempts = 0
            delay(FETCH_SCAN_STATE_DELAY_MILLIS)
        }
    }.flowOn(bgDispatcher)

    private suspend fun FlowCollector<FetchScanState>.handleError(
        retryAttempts: Int,
        failure: Failure
    ): Boolean {
        return if (retryAttempts >= MAX_RETRY) {
            AppLog.d(T.JETPACK_BACKUP, "$tag: Exceeded $MAX_RETRY retries while fetching status")
            emit(failure)
            true
        } else {
            delay(DELAY_MILLIS * (max(1, DELAY_FACTOR * retryAttempts)))
            false
        }
    }

    sealed class FetchScanState {
        data class Success(val scanStateModel: ScanStateModel) : FetchScanState()
        sealed class Failure : FetchScanState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
            object MultisiteNotSupported : Failure()
            object VaultPressActiveOnSite : Failure()
        }
    }
}
