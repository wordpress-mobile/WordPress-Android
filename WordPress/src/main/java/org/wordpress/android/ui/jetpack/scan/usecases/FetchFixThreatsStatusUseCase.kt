package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel
import org.wordpress.android.fluxc.model.scan.threat.FixThreatStatusModel.FixStatus
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchFixThreatsStatusPayload
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.Complete
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState.InProgress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

private const val FETCH_FIX_THREATS_STATUS_DELAY_MILLIS = 5000L

class FetchFixThreatsStatusUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchFixThreatsStatus(
        remoteSiteId: Long,
        fixableThreatIds: List<Long>,
        delayInMs: Long = FETCH_FIX_THREATS_STATUS_DELAY_MILLIS
    ): Flow<FetchFixThreatsState> = flow {
        while (true) {
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                emit(Failure.NetworkUnavailable)
                return@flow
            }

            val result = scanStore.fetchFixThreatsStatus(FetchFixThreatsStatusPayload(remoteSiteId, fixableThreatIds))
            if (result.isError) {
                emit(Failure.RemoteRequestFailure)
                return@flow
            } else {
                val fixState = mapToFixState(result.fixThreatStatusModels, fixableThreatIds)
                emit(fixState)
                if (fixState == InProgress) {
                    delay(delayInMs)
                } else {
                    return@flow
                }
            }
        }
    }.flowOn(ioDispatcher)

    private fun mapToFixState(models: List<FixThreatStatusModel>, fixableThreatIds: List<Long>): FetchFixThreatsState {
        val isFixing = models.any { it.status == FixStatus.IN_PROGRESS }
        val isFixingComplete = models.filter { it.status == FixStatus.FIXED }.size == fixableThreatIds.size
        val containsError = models.any { it.error != null }

        return when {
            isFixing -> InProgress
            isFixingComplete -> Complete
            else -> {
                // TODO ashiagr replace AppLog tag
                if (containsError) AppLog.e(T.API, models.filter { it.error != null }.toString())
                Failure.FixFailure
            }
        }
    }

    sealed class FetchFixThreatsState {
        object InProgress : FetchFixThreatsState()
        object Complete : FetchFixThreatsState()
        sealed class Failure : FetchFixThreatsState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
            object FixFailure : Failure()
        }
    }
}
