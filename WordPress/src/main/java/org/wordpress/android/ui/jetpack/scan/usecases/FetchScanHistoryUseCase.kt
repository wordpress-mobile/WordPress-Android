package org.wordpress.android.ui.jetpack.scan.usecases

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryPayload
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class FetchScanHistoryUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val scanStore: ScanStore
) {
    suspend fun fetch(site: SiteModel): FetchScanHistoryState {
        return if (!networkUtilsWrapper.isNetworkAvailable()) {
            FetchScanHistoryState.Failure.NetworkUnavailable
        } else {
            val result = scanStore.fetchScanHistory(FetchScanHistoryPayload(site))
            if (result.isError) {
                FetchScanHistoryState.Failure.RemoteRequestFailure
            } else {
                FetchScanHistoryState.Success
            }
        }
    }

    sealed class FetchScanHistoryState {
        object Success : FetchScanHistoryState()
        sealed class Failure : FetchScanHistoryState() {
            object NetworkUnavailable : Failure()
            object RemoteRequestFailure : Failure()
        }
    }
}
