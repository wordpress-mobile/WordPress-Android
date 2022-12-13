package org.wordpress.android.ui.jetpack.scan.usecases

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_HISTORY
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryError
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ScanStore.OnScanHistoryFetched
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.ScanTracker

@InternalCoroutinesApi
class FetchScanHistoryUseCaseTest : BaseUnitTest() {
    private lateinit var fetchScanHistoryUseCase: FetchScanHistoryUseCase

    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var scanTracker: ScanTracker
    private val site: SiteModel = SiteModel()

    @Before
    fun setUp() {
        fetchScanHistoryUseCase = FetchScanHistoryUseCase(networkUtilsWrapper, scanStore, scanTracker, TEST_DISPATCHER)

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `Network failure returned, when the device is offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = fetchScanHistoryUseCase.fetch(site)

        assertThat(result).isInstanceOf(Failure.NetworkUnavailable::class.java)
    }

    @Test
    fun `Network error tracked, when the device is offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        fetchScanHistoryUseCase.fetch(site)

        verify(scanTracker).trackOnError(ScanTracker.ErrorAction.FETCH_SCAN_HISTORY, ScanTracker.ErrorCause.OFFLINE)
    }

    @Test
    fun `Request failure returned, when the request fails`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(site.siteId, FetchScanHistoryError(GENERIC_ERROR), FETCH_SCAN_HISTORY)
        )

        val result = fetchScanHistoryUseCase.fetch(site)

        assertThat(result).isInstanceOf(Failure.RemoteRequestFailure::class.java)
    }

    @Test
    fun `Request failure tracked, when the request fails`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(site.siteId, FetchScanHistoryError(GENERIC_ERROR), FETCH_SCAN_HISTORY)
        )

        fetchScanHistoryUseCase.fetch(site)

        verify(scanTracker).trackOnError(ScanTracker.ErrorAction.FETCH_SCAN_HISTORY, ScanTracker.ErrorCause.REMOTE)
    }

    @Test
    fun `Data from db returned, when the request succeeds`() = test {
        val threats = listOf<ThreatModel>(mock(), mock())
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(site.siteId, null, FETCH_SCAN_HISTORY)
        )
        whenever(scanStore.getScanHistoryForSite(site)).thenReturn(threats)

        val result = fetchScanHistoryUseCase.fetch(site)

        assertThat((result as Success).threatModels).isEqualTo(threats)
    }
}
