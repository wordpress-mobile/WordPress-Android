package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.ScanTracker
import javax.inject.Inject
import javax.inject.Named

class FetchScanHistoryUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore,
    private val scanTracker: ScanTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun fetch(
        site: SiteModel
    ): FetchScanHistoryState {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            scanTracker.trackOnError(ScanTracker.ErrorAction.FETCH_SCAN_HISTORY, ScanTracker.ErrorCause.OFFLINE)
            FetchScanHistoryState.Failure.NetworkUnavailable
        } else {
            val result = scanStore.fetchScanHistory(FetchScanHistoryPayload(site))
            if (result.isError) {
                scanTracker.trackOnError(ScanTracker.ErrorAction.FETCH_SCAN_HISTORY, ScanTracker.ErrorCause.REMOTE)
                FetchScanHistoryState.Failure.RemoteRequestFailure
            } else {
                withContext(bgDispatcher) {
                    FetchScanHistoryState.Success(scanStore.getScanHistoryForSite(site))
                }
            }
        }
    }

    sealed class FetchScanHistoryState {
        data class Success(val threatModels: List<ThreatModel>) : FetchScanHistoryState()
        sealed class Failure : FetchScanHistoryState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
        }
    }
}
