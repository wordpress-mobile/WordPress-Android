package org.wordpress.android.ui.jetpack.scan.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ScanAction.START_SCAN
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.OnScanStarted
import org.wordpress.android.fluxc.store.ScanStore.ScanStartError
import org.wordpress.android.fluxc.store.ScanStore.ScanStartErrorType
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
class StartScanUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: StartScanUseCase
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStateModel: ScanStateModel
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var scanStore: ScanStore

    @Before
    fun setup() {
        useCase = StartScanUseCase(networkUtilsWrapper, scanStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModel)
    }

    @Test
    fun `given site and no network, when scan is started, then NetworkUnavailable is returned`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.startScan(site).toList(mutableListOf())

        assertThat(result).contains(StartScanState.Failure.NetworkUnavailable)
    }

    @Test
    fun `given site, when scan is started, then Success is returned on success`() = testWithSuccessResponse {
        val result = useCase.startScan(site).toList(mutableListOf())

        assertThat(result).contains(StartScanState.Success)
    }

    @Test
    fun `when scan start is triggered, then scan starts optimistically by updating scanning scan state in db`() =
        testWithSuccessResponse {
            val scanStateModelInIdleState = ScanStateModel(state = ScanStateModel.State.IDLE)
            val expectedScanStateModel = scanStateModelInIdleState.copy(state = ScanStateModel.State.SCANNING)
            whenever(scanStore.getScanStateForSite(any())).thenReturn(scanStateModel)

            val result = useCase.startScan(site).toList(mutableListOf())

            verify(scanStore).addOrUpdateScanStateModelForSite(site, expectedScanStateModel)
            assertThat(result).contains(StartScanState.ScanningStateUpdatedInDb(expectedScanStateModel))
        }

    @Test
    fun `given server is unavailable, when scan is started, then RemoteRequestFailure is returned`() =
        testWithErrorResponse {
            val result = useCase.startScan(site).toList(mutableListOf())

            assertThat(result).contains(StartScanState.Failure.RemoteRequestFailure)
        }

    private fun <T> testWithSuccessResponse(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(scanStore.startScan(any())).thenReturn(OnScanStarted(START_SCAN))
            block()
        }
    }

    private fun <T> testWithErrorResponse(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(scanStore.startScan(any())).thenReturn(
                OnScanStarted(ScanStartError(ScanStartErrorType.GENERIC_ERROR), START_SCAN)
            )
            block()
        }
    }
}
