package org.wordpress.android.ui.jetpack.scan

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success

@InternalCoroutinesApi
class ScanViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStateItemsBuilder: ScanStateListItemsBuilder
    @Mock private lateinit var fetchScanStateUseCase: FetchScanStateUseCase
    private lateinit var viewModel: ScanViewModel

    private val fakeScanStateModel = ScanStateModel(state = ScanStateModel.State.IDLE, hasCloud = true)

    @Before
    fun setUp() = test {
        viewModel = ScanViewModel(
            scanStateItemsBuilder,
            fetchScanStateUseCase,
            TEST_DISPATCHER,
            TEST_DISPATCHER
        )
        whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Success(fakeScanStateModel)))
    }

    @Test
    fun `when vm starts, fetch scan state is triggered`() = test {
        viewModel.start(site)

        verify(fetchScanStateUseCase).fetchScanState(site)
    }

    @Test
    fun `when scan state is fetched successfully, then ui is updated with content`() = test {
        val uiStates = init().uiStates

        assertThat(uiStates.last()).isInstanceOf(Content::class.java)
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        viewModel.start(site)

        return Observers(uiStates)
    }

    private data class Observers(val uiStates: List<UiState>)
}
