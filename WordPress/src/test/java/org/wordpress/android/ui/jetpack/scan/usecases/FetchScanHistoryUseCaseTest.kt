package org.wordpress.android.ui.jetpack.scan.usecases

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_HISTORY
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryError
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ScanStore.OnScanHistoryFetched
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanHistoryUseCase.FetchScanHistoryState.Success
import org.wordpress.android.util.NetworkUtilsWrapper

class FetchScanHistoryUseCaseTest : BaseUnitTest() {
    private lateinit var fetchScanHistoryUseCase: FetchScanHistoryUseCase

    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var scanStore: ScanStore
    private val site: SiteModel = SiteModel()

    @Before
    fun setUp() {
        fetchScanHistoryUseCase = FetchScanHistoryUseCase(networkUtilsWrapper, scanStore)

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun `Network failure returned, when the device is offline`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = fetchScanHistoryUseCase.fetch(site)

        assertThat(result).isInstanceOf(Failure.NetworkUnavailable::class.java)
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
    fun `Success returned, when the request succeeds`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(site.siteId, null, FETCH_SCAN_HISTORY)
        )

        val result = fetchScanHistoryUseCase.fetch(site)

        assertThat(result).isInstanceOf(Success::class.java)
    }
}
