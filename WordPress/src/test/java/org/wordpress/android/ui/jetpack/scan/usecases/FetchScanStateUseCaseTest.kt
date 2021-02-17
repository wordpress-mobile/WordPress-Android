package org.wordpress.android.ui.jetpack.scan.usecases

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.ScanAction.FETCH_SCAN_STATE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.OnScanStateFetched
import org.wordpress.android.fluxc.store.ScanStore.ScanStateError
import org.wordpress.android.fluxc.store.ScanStore.ScanStateErrorType
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Failure.RemoteRequestFailure
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class FetchScanStateUseCaseTest : BaseUnitTest() {
    private lateinit var useCase: FetchScanStateUseCase
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStateModel: ScanStateModel
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var scanStore: ScanStore

    @Before
    fun setup() = test {
        useCase = FetchScanStateUseCase(networkUtilsWrapper, scanStore, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModel)
    }

    @Test
    fun `given site, when scan state is fetched, then NetworkUnavailable is returned on no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val result = useCase.fetchScanState(site).toList(mutableListOf())

        assertThat(result).contains(FetchScanState.Failure.NetworkUnavailable)
    }

    @Test
    fun `given site, when scan state is fetched, then Success is returned on success`() = test {
        whenever(scanStore.fetchScanState(any())).thenReturn(OnScanStateFetched(FETCH_SCAN_STATE))

        val result = useCase.fetchScanState(site).toList(mutableListOf())

        assertThat(result).contains(Success(scanStateModel))
    }

    @Test
    fun `given site, when scan state is fetched, then RemoteRequestFailure is returned on failure`() = test {
        whenever(scanStore.fetchScanState(any())).thenReturn(
                OnScanStateFetched(ScanStateError(ScanStateErrorType.GENERIC_ERROR), FETCH_SCAN_STATE)
        )
        val result = useCase.fetchScanState(site).toList(mutableListOf())

        assertThat(result).contains(FetchScanState.Failure.RemoteRequestFailure)
    }

    @Test
    fun `when SCANNING scan state is fetched, then polling occurs until IDLE state is returned on success`() = test {
        whenever(scanStore.fetchScanState(any())).thenReturn(OnScanStateFetched(FETCH_SCAN_STATE))
        val scanStateScanningModel = ScanStateModel(state = ScanStateModel.State.SCANNING)
        val scanStateModels = listOf(
                scanStateScanningModel,
                scanStateScanningModel,
                ScanStateModel(state = ScanStateModel.State.IDLE)
        )
        whenever(scanStore.getScanStateForSite(any()))
                .thenReturn(scanStateModels[0])
                .thenReturn(scanStateModels[1])
                .thenReturn(scanStateModels[2])

        val result = useCase.fetchScanState(site = site).toList(mutableListOf())

        verify(scanStore, times(scanStateModels.size)).fetchScanState(any())
        assertThat(result).containsSequence(scanStateModels.map { Success(it) })
    }

    @Test
    fun `when SCANNING scan state is fetched, then polling occurs until error is returned on failure`() = test {
        val scanStateScanningModel = ScanStateModel(state = ScanStateModel.State.SCANNING)
        val scanStateError = ScanStateError(ScanStateErrorType.GENERIC_ERROR)
        whenever(scanStore.getScanStateForSite(any())).thenReturn(scanStateScanningModel)
        whenever(scanStore.fetchScanState(any())).thenReturn(
                OnScanStateFetched(FETCH_SCAN_STATE),
                OnScanStateFetched(scanStateError, FETCH_SCAN_STATE)
        )

        val result = useCase.fetchScanState(site = site).toList(mutableListOf())

        // one success and 1+MAX_RETRY attempts
        verify(scanStore, times(5)).fetchScanState(any())
        assertThat(result).containsSequence(
                Success(scanStateScanningModel),
                FetchScanState.Failure.RemoteRequestFailure
        )
    }

    @Test
    fun `given max fetch retries exceeded, when scan state triggers, then return remote request failure`() = test {
        val scanStateError = ScanStateError(ScanStateErrorType.GENERIC_ERROR)
        whenever(scanStore.fetchScanState(any())).thenReturn(OnScanStateFetched(scanStateError, FETCH_SCAN_STATE))

        val result = useCase.fetchScanState(site = site).toList(mutableListOf())

        verify(scanStore, times(MAX_RETRY + 1)).fetchScanState(anyOrNull())
        assertThat(result).size().isEqualTo(1)
        assertThat(result).isEqualTo(listOf(RemoteRequestFailure))
    }

    @Test
    fun `given fetch error under retry count, when scan state triggers, then return only success`() = test {
        val scanStateScanningModel = ScanStateModel(state = ScanStateModel.State.SCANNING)
        val scanStateFinished = ScanStateModel(state = ScanStateModel.State.IDLE)
        val scanStateError = ScanStateError(ScanStateErrorType.GENERIC_ERROR)
        whenever(scanStore.fetchScanState(any()))
                .thenReturn(OnScanStateFetched(scanStateError, FETCH_SCAN_STATE))
                .thenReturn(OnScanStateFetched(scanStateError, FETCH_SCAN_STATE))
                .thenReturn(OnScanStateFetched(FETCH_SCAN_STATE))

        whenever(scanStore.getScanStateForSite(site))
                .thenReturn(scanStateScanningModel)
                .thenReturn(scanStateFinished)

        val result = useCase.fetchScanState(site = site).toList(mutableListOf())

        assertThat(result).allSatisfy { it is Success }
    }
}
