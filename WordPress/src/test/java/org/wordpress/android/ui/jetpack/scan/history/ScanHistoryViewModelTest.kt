package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryError
import org.wordpress.android.fluxc.store.ScanStore.FetchScanHistoryErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ScanStore.OnScanHistoryFetched
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.ALL
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.FIXED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType.IGNORED
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState.NoConnection
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.UiState.ErrorUiState.RequestFailed
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ScanHistoryViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private val site: SiteModel = SiteModel()

    private lateinit var viewModel: ScanHistoryViewModel

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryViewModel(scanStore, networkUtilsWrapper, TEST_DISPATCHER)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(OnScanHistoryFetched(1L, mock()))
        whenever(scanStore.getScanHistoryForSite(anyOrNull())).thenReturn(listOf(mock()))
    }

    @Test
    fun `Threats loaded, when the user opens the screen`() = test {
        viewModel.start(site)

        assertThat(viewModel.threats.value!!.size).isEqualTo(1)
    }

    @Test
    fun `Content shown, when fetching threats`() = test {
        val observers = init()

        assertThat(observers.uiStates[0]).isInstanceOf(ContentUiState::class.java)
    }

    @Test
    fun `All tabs shown, when content displayed`() = test {
        val observers = init()

        assertThat((observers.uiStates[0] as ContentUiState).tabs.map { it.type }).isEqualTo(
                listOf(ALL, FIXED, IGNORED)
        )
    }

    @Test
    fun `No connection error shown, when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val observers = init()

        assertThat(observers.uiStates.last()).isInstanceOf(NoConnection::class.java)
    }

    @Test
    fun `RequestFailed error shown, when network request fails`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(1L, FetchScanHistoryError(GENERIC_ERROR, ""), mock())
        )

        val observers = init()

        assertThat(observers.uiStates.last()).isInstanceOf(RequestFailed::class.java)
    }

    @Test
    fun `Threats fetched, when user clicks on Retry button on RequestFailedError screen`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val observers = init()

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        (observers.uiStates.last() as NoConnection).retry.invoke()

        assertThat(viewModel.threats.value!!.size).isEqualTo(1)
    }

    @Test
    fun `Threats fetched, when user clicks on Retry button on NoConnectionError screen`() = test {
        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(
                OnScanHistoryFetched(1L, FetchScanHistoryError(GENERIC_ERROR, ""), mock())
        )
        val observers = init()

        whenever(scanStore.fetchScanHistory(anyOrNull())).thenReturn(OnScanHistoryFetched(1L, mock()))
        (observers.uiStates.last() as RequestFailed).retry.invoke()

        assertThat(viewModel.threats.value!!.size).isEqualTo(1)
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.start(site)
        return Observers(uiStates)
    }

    private data class Observers(
        val uiStates: List<UiState>
    )
}
